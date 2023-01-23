/*
 * Copyright 2012, UCAR/Unidata.
 * See the LICENSE file for more information.
 */

package dap4.servlet;

import dap4.core.ce.CEConstraint;
import dap4.core.util.DapContext;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


/**
 * Provide an LRU cache of open CDM sources.
 * The cache key is assumed to be the CDM object.
 * The cache is synchronized to avoid race conditions.
 * Note that we do not have to release because Java
 * uses garbage collection and entries will be purged
 * if the LRU cache is full.
 * Singleton class
 */

abstract public class DapCache {

  //////////////////////////////////////////////////
  // Constants

  static final int MAXFILES = 100; // size of the cache

  static public final String MATCHMETHOD = "dspMatch";

  //////////////////////////////////////////////////
  // Static variables

  /**
   * Define an lru cache of known CDMWrap objects: oldest first.
   */
  static protected List<CDMWrap> lru = new ArrayList<>();

  static public synchronized CDMWrap open(String path, DapContext cxt) throws IOException {
    assert cxt != null;
    int lrusize = lru.size();
    for (int i = lrusize - 1; i >= 0; i--) {
      CDMWrap c4 = lru.get(i);
      String c4path = c4.getLocation();
      if (c4path.equals(path)) {
        // move to the front of the queue to maintain LRU property
        lru.remove(i);
        lru.add(c4);
        CEConstraint.release(lru.get(0).getDMR());
        return c4;
      }
    }
    // No match found, create and initialize it.
    // If cache is full, remove oldest entry
    if (lrusize == MAXFILES) {
      // make room
      lru.remove(0);
      CEConstraint.release(lru.get(0).getDMR());
    }
    // Create the wrapper
    CDMWrap c4 = new CDMWrap();
    c4.open(path);
    lru.add(c4);
    return c4;
  }

  static synchronized public void flush() // for testing
      throws Exception {
    while (lru.size() > 0) {
      CDMWrap c4 = lru.get(0);
      CEConstraint.release(c4.getDMR());
      c4.close();
      lru.remove(0);
    }
  }


} // DapCache
