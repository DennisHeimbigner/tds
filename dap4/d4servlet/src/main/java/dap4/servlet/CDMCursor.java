/*
 * Copyright 2012, UCAR/Unidata.
 * See the LICENSE file for more information.
 */

package dap4.servlet;

import dap4.core.dmr.*;
import dap4.core.interfaces.ArrayScheme;
import dap4.core.util.*;
import dap4.dap4lib.D4Index;
import dap4.dap4lib.cdm.CDMTypeFcns;
import dap4.dap4lib.cdm.CDMUtil;

import ucar.ma2.*;

import java.util.List;

public class CDMCursor {

  //////////////////////////////////////////////////
  // Instance variables
  protected ArrayScheme scheme;
  protected CDMWrap cdmwrap;
  protected DapNode template;
  protected CDMCursor container;

  protected ucar.ma2.Array array = null;
  protected ucar.ma2.StructureData structdata = null; // scheme == STRUCTURE
  ucar.ma2.StructureMembers.Member member = null; // for field cursors

  protected long recordindex = -1;
  protected long recordcount = -1;
  protected Index index = null;

  //////////////////////////////////////////////////
  // Constructor(s)

  public CDMCursor(ArrayScheme scheme, CDMWrap c4, DapNode template, CDMCursor container) throws DapException {
    this.scheme = scheme;
    this.cdmwrap = c4;
    this.template = template;
    this.container = container;
  }

  public CDMCursor(CDMCursor c) {
    assert false;
    this.array = c.array;
    this.structdata = c.structdata;
    this.member = c.member;

  }

  //////////////////////////////////////////////////
  // Get/Set

  public ArrayScheme getScheme() {
    return this.scheme;
  }

  public CDMWrap getCDMDAP4() {
    return this.cdmwrap;
  }

  public DapNode getTemplate() {
    return this.template;
  }

  public CDMCursor getContainer() {
    return this.container;
  }

  public long getRecordIndex() {
    return this.recordindex;
  }

  public long getRecordCount() {
    return this.recordcount;
  }

  public Index getIndex() {
    return this.index;
  }

  public CDMCursor setRecordIndex(long index) {
    this.recordindex = index;
    return this;
  }

  public CDMCursor setRecordCount(long count) {
    this.recordcount = count;
    return this;
  }

  public CDMCursor setIndex(Index count) {
    this.index = count;
    return this;
  }

  //////////////////////////////////////////////////

  public Object read(List<Slice> slices) throws DapException {
    switch (this.scheme) {
      case ATOMIC:
        return readAtomic(slices);
      case STRUCTURE:
        if (((DapVariable) this.getTemplate()).getRank() > 0 || DapUtil.isScalarSlices(slices))
          throw new DapException("Cannot slice a scalar variable");
        CDMCursor[] instances = new CDMCursor[1];
        instances[0] = this;
        return instances;
      case SEQUENCE:
        if (((DapVariable) this.getTemplate()).getRank() > 0 || DapUtil.isScalarSlices(slices))
          throw new DapException("Cannot slice a scalar variable");
        instances = new CDMCursor[1];
        instances[0] = this;
        return instances;
      case STRUCTARRAY:
        Odometer odom = OdometerFactory.factory(slices, ((DapVariable) this.getTemplate()).getDimensions());
        instances = new CDMCursor[(int) odom.totalSize()];
        for (int i = 0; odom.hasNext(); i++) {
          instances[i] = readStructure(odom.next());
        }
        return instances;
      case SEQARRAY:
        instances = readSequence(slices);
        return instances;
      default:
        throw new DapException("Attempt to slice a scalar object");
    }
  }

  public Object read(Index index) throws DapException {
    return read(D4Index.indexToSlices(index));
  }

  public CDMCursor readField(int findex) throws DapException {
    if (this.scheme != scheme.RECORD && this.scheme != scheme.STRUCTURE)
      throw new DapException("Illegal cursor scheme for readfield()");
    DapVariable var = (DapVariable) getTemplate();
    DapStructure basetype = (DapStructure) var.getBaseType();
    if (findex < 0 || findex >= basetype.getFields().size())
      throw new DapException("Field index out of range: " + findex);
    CDMCursor fieldcursor = null;
    if (this.scheme == ArrayScheme.RECORD) {
      DapSequence seq = (DapSequence) basetype;
      DapVariable field = seq.getField(0);
      DapType fieldtype = field.getBaseType();
      switch (fieldtype.getTypeSort()) {
        default: // atomic
          DataType cdmfieldtype = CDMTypeFcns.daptype2cdmtype(fieldtype);
          if (cdmfieldtype == null)
            throw new dap4.core.util.DapException("Unknown field type: " + fieldtype);
          int ri = (int) this.recordindex;
          Object o = array.getObject(ri);
          Array fielddata = CDMTypeFcns.arrayify(cdmfieldtype, o); // not very efficient; should do conversion
          fieldcursor = new CDMCursor(ArrayScheme.ATOMIC, getCDMDAP4(), field, this);
          fieldcursor.setArray(fielddata);
          break;
        case Sequence:
        case Structure:
          break;
      }
    } else { // scheme == STRUCTURE
      assert this.structdata != null;
      fieldcursor = getFieldCursor(this, findex);
    }
    return fieldcursor;
  }

  protected CDMCursor getFieldCursor(CDMCursor container, int findex) throws DapException {
    // Now, create a cursors for a field f this instance
    DapVariable var = (DapVariable) getTemplate();
    DapStructure type = (DapStructure) var.getBaseType();
    DapVariable field = (DapVariable) type.getFields().get(findex);
    DapType ftype = field.getBaseType();
    ArrayScheme scheme = schemeFor(field);
    CDMCursor fc = new CDMCursor(scheme, (CDMWrap) getCDMDAP4(), field, this);
    StructureMembers.Member member = this.structdata.getStructureMembers().getMember(findex);
    fc.setMember(member);
    fc.setArray(this.structdata.getArray(fc.member));
    return fc;
  }

  public boolean isScalar() {
    if (getTemplate().getSort().isVar()) {
      return ((DapVariable) getTemplate()).getRank() == 0;
    } else
      return false;
  }


  public boolean isField() {
    return getTemplate().getContainer() != null;
  }

  public boolean isAtomic() {
    boolean is = this.scheme == ArrayScheme.ATOMIC;
    assert !is || getTemplate().getSort() == DapSort.ATOMICTYPE || (getTemplate().getSort() == DapSort.VARIABLE
        && ((DapVariable) getTemplate()).getBaseType().getTypeSort().isAtomic());
    return is;
  }

  public boolean isCompound() {
    boolean is = (this.scheme == ArrayScheme.SEQUENCE || this.scheme == ArrayScheme.STRUCTURE);
    assert !is || getTemplate().getSort() == DapSort.SEQUENCE || getTemplate().getSort() == DapSort.STRUCTURE
        || (getTemplate().getSort() == DapSort.VARIABLE
            && ((DapVariable) getTemplate()).getBaseType().getTypeSort().isCompoundType());
    return is;
  }

  public boolean isCompoundArray() {
    boolean is = (this.scheme == ArrayScheme.SEQARRAY || this.scheme == ArrayScheme.STRUCTARRAY);
    assert !is || getTemplate().getSort() == DapSort.SEQUENCE || getTemplate().getSort() == DapSort.STRUCTURE
        || (getTemplate().getSort() == DapSort.VARIABLE
            && ((DapVariable) getTemplate()).getBaseType().getTypeSort().isCompoundType());
    return is;
  }

  protected static ArrayScheme schemeFor(DapVariable field) {
    DapType ftype = field.getBaseType();
    ArrayScheme scheme = null;
    boolean isscalar = field.getRank() == 0;
    if (ftype.getTypeSort().isAtomic())
      scheme = ArrayScheme.ATOMIC;
    else {
      if (ftype.getTypeSort().isStructType())
        scheme = ArrayScheme.STRUCTARRAY;
      else if (ftype.getTypeSort().isSeqType())
        scheme = ArrayScheme.SEQARRAY;
    }
    return scheme;
  }

  public CDMCursor readRecord(long i) throws DapException {
    if (this.scheme != scheme.SEQUENCE)
      throw new DapException("Attempt to read record from non-sequence cursor");
    if (i < 0 || i >= this.recordcount)
      throw new DapException("Record index out of bounds");
    DapVariable var = (DapVariable) getTemplate();
    CDMCursor c = new CDMCursor(ArrayScheme.RECORD, getCDMDAP4(), var, this);
    c.setArray(this.array);
    c.setRecordIndex(i);
    return c;
  }

  public int fieldIndex(String name) throws DapException {
    DapStructure ds;
    if (getTemplate().getSort().isCompound())
      ds = (DapStructure) getTemplate();
    else if (getTemplate().getSort().isVar() && (((DapVariable) getTemplate()).getBaseType().getSort().isCompound()))
      ds = (DapStructure) ((DapVariable) getTemplate()).getBaseType();
    else
      throw new DapException("Attempt to get field name on non-compound object");
    int i = ds.indexByName(name);
    if (i < 0)
      throw new DapException("Unknown field name: " + name);
    return i;
  }


  //////////////////////////////////////////////////
  // Support Methods

  protected Object readAtomic(List<Slice> slices) throws DapException {
    if (slices == null)
      throw new DapException("CDMCursor.read: null set of slices");
    assert (this.scheme == scheme.ATOMIC);
    DapVariable atomvar = (DapVariable) getTemplate();
    assert slices != null && ((atomvar.getRank() == 0 && slices.size() == 1) || (slices.size() == atomvar.getRank()));
    return sliceAtomic(slices, this.array, atomvar);
  }

  protected Object sliceAtomic(List<Slice> slices, Array array, DapVariable var) throws DapException {
    List<DapDimension> dimset = var.getDimensions();
    DapType basetype = var.getBaseType();
    // If content.getDataType returns object, then we
    // really do not know its true datatype. So, as a rule,
    // we will rely on this.basetype.
    DataType datatype = CDMTypeFcns.daptype2cdmtype(basetype);
    if (datatype == null)
      throw new dap4.core.util.DapException("Unknown basetype: " + basetype);
    Object content = array.get1DJavaArray(datatype); // not very efficient; should do conversion
    Odometer odom = OdometerFactory.factory(slices, dimset);
    Object data = CDMTypeFcns.createVector(datatype, odom.totalSize());
    for (int dstoffset = 0; odom.hasNext(); dstoffset++) {
      D4Index index = odom.next();
      long srcoffset = index.index();
      CDMTypeFcns.vectorcopy(basetype, content, data, srcoffset, dstoffset);
    }
    return data;
  }


  protected CDMCursor readStructure(Index index) throws DapException {
    assert (index != null);
    DapVariable var = (DapVariable) getTemplate();
    DapStructure type = (DapStructure) var.getBaseType();
    int pos = index.currentElement();
    if (pos < 0 || pos > var.getCount())
      throw new IndexOutOfBoundsException("read: " + index);
    ArrayStructure sarray = (ArrayStructure) this.array;
    CDMCursor instance;
    assert (this.scheme == scheme.STRUCTARRAY);
    ucar.ma2.StructureData sd = sarray.getStructureData((int) pos);
    assert sd != null;
    instance = new CDMCursor(ArrayScheme.STRUCTURE, getCDMDAP4(), var, null).setStructureData(sd);
    instance.setIndex(index);
    return instance;
  }

  protected CDMCursor[] readSequence(List<Slice> slices) throws DapException {
    assert (this.scheme == scheme.SEQARRAY);
    DapVariable var = (DapVariable) getTemplate();
    DapSequence type = (DapSequence) var.getBaseType();
    // new CDMCursor(Scheme.SEQUENCE, (CDMDSP) this.dsp, var, this);
    CDMCursor[] instances = new CDMCursor[(int) DapUtil.sliceProduct(slices)];
    Array seqarray = this.array;
    if (var.getRank() == 0) {// scalar
      if (!DapUtil.isScalarSlices(slices))
        throw new DapException("Non-scalar slice set applied to scalar variable");
      instances[0] = new CDMCursor(ArrayScheme.SEQUENCE, getCDMDAP4(), var, this);
      instances[0].setArray(seqarray);
      instances[0].setRecordCount(seqarray.getSize());
    } else {
      List<Range> rlist = CDMUtil.createCDMRanges(slices);
      Array instancearray;
      try {
        instancearray = seqarray.section(rlist);
      } catch (InvalidRangeException e) {
        throw new DapException("Illegal slice set", e);
      }
      // extracted via List<Slice>. IN theory, this should be an array of arrays,
      // but if the sequence field basetype is atomic, thenit is an array
      // of atomic values.
      int slicecount = (int) DapUtil.sliceProduct(slices);
      for (int i = 0; i < slicecount; i++) {
        Array ao = (Array) instancearray.getObject(i);
        CDMCursor c = new CDMCursor(ArrayScheme.SEQUENCE, getCDMDAP4(), var, this);
        c.setArray(ao);
        long rcount = ao.getSize();
        c.setRecordCount(rcount);
        instances[i] = c;
      }
    }
    return instances;
  }

  //////////////////////////////////////////////////
  // CDMCursor Extensions

  public CDMCursor setArray(ucar.ma2.Array a) {
    this.array = a;
    return this;
  }

  public ucar.ma2.Array getArray() {
    return this.array;
  }

  public CDMCursor setStructureData(ucar.ma2.StructureData sd) {
    this.structdata = sd;
    return this;
  }

  public CDMCursor setMember(ucar.ma2.StructureMembers.Member m) {
    this.member = m;
    return this;
  }


}
