/**
 * <copyright>
 *
 * Copyright (c) 2004 IBM Corporation and others.
 * All rights reserved.   This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: 
 *   IBM - Initial API and implementation
 *
 * </copyright>
 *
 * $Id: TestUtil.java,v 1.1.2.2 2005/06/08 18:27:46 nickb Exp $
 */
package org.eclipse.emf.test.core;

import java.io.File;
import java.net.URL;
import java.util.List;

import org.eclipse.core.runtime.Platform;

/**
 * @author marcelop
 */
public class TestUtil
{
  private static class Foo{};
  
  public static String getPluginDirectory()
  {
    try
    {
      if (EMFTestCorePlugin.getPlugin() != null)
      {
        return new java.io.File(Platform.asLocalURL(EMFTestCorePlugin.getPlugin().getBundle().getEntry("/")).getFile()).toString();
      }
    }
    catch (Throwable t)
    {
    }
    
    URL url = new Foo().getClass().getResource(".");
    String path = url.getPath();
    path = path.substring(0, path.indexOf("org.eclipse.emf.test.core/") + "org.eclipse.emf.test.core/".length());
    return new File(path).getAbsolutePath();
  }
  
  public static boolean areEqual(List list1, List list2)
  {
    if (list1 == null) return list2 == null;
    if (list2 == null) return false; 

    int size = list1.size(); 
    if (size != list2.size())
    {
      return false;
    }
    
    for(int i=0; i<size; i++)
    {
      if (list1.get(i) != list2.get(i))
      {
        if (list1.get(i) == null || list2.get(i) == null)
        {
          return false;
        }
        else if (!list1.get(i).equals(list2.get(i)))
        {
          return false; 
        }
      }
    }
    
    return true;  
  }  
}