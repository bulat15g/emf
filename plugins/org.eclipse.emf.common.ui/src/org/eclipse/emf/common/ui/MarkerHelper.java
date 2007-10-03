/**
 * <copyright>
 *
 * Copyright (c) 2006 IBM Corporation and others.
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
 * $Id: MarkerHelper.java,v 1.5 2006/07/17 17:05:27 marcelop Exp $
 */
package org.eclipse.emf.common.ui;

import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.part.FileEditorInput;

import org.eclipse.emf.common.util.Diagnostic;
import org.eclipse.emf.common.util.URI;

/**
 * Provides methods to simplify the work with {@link IMarker}s.  The main goal is to
 * simplify the creation of markers using the information described in 
 * {@link Diagnostic}s. 
 * 
 * @since 2.2.0
 */
public class MarkerHelper
{
  protected String getMarkerID()
  {
    return "org.eclipse.core.resources.problemmarker";
  }  
  
  protected IFile getFile(Object datum)
  {
    if (datum instanceof IFileEditorInput)
    {
      return ((IFileEditorInput)datum).getFile();
    }
    else if (datum instanceof IFile)
    {
      return (IFile)datum;
    }
    else if (datum instanceof Diagnostic)
    {
      return getFile((Diagnostic)datum);
    }
    return null;
  }

  protected IFile getFile(Diagnostic diagnostic)
  {
    List data = diagnostic.getData();
    if (data != null)
    {
      for (Iterator i = data.iterator(); i.hasNext(); )
      {
        Object datum = i.next();
        IFile result = getFile(datum);
        if (result != null)
        {
          return result;
        }
      }
    }
    return null;
  }

  protected IFile getFile(URI uri)
  {
    String scheme = uri.scheme();
    if ("platform".equals(scheme) && uri.segmentCount() > 1 && "resource".equals(uri.segment(0)))
    {
      StringBuffer platformResourcePath = new StringBuffer();
      for (int j = 1, size = uri.segmentCount(); j < size; ++j)
      {
        platformResourcePath.append('/');
        platformResourcePath.append(URI.decode(uri.segment(j)));
      }
      return ResourcesPlugin.getWorkspace().getRoot().getFile(new Path(platformResourcePath.toString()));
    }
    return null;
  }

  /**
   * <p>Creates a marker based on the information available in the specified
   * diagnostic.  The marker's id is defined by {@link #getMarkerID()}.</p>
   * <p>The default implementation looks in the diagnostic's data array for
   * objects that would allow an IFile to be computed</p> 
   * @param diagnostic
   * @throws CoreException
   */
  public void createMarkers(Diagnostic diagnostic) throws CoreException
  {
    if (diagnostic.getChildren().isEmpty())
    {
      createMarkers(getFile(diagnostic), diagnostic, null);
    }
    else if (diagnostic.getMessage() == null)
    {
      for (Iterator i = diagnostic.getChildren().iterator(); i.hasNext(); )
      {
        createMarkers((Diagnostic)i.next());
      }
    }
    else
    {
      for (Iterator i = diagnostic.getChildren().iterator(); i.hasNext(); )
      {
        Diagnostic childDiagnostic = (Diagnostic)i.next();
        createMarkers(getFile(childDiagnostic), childDiagnostic, diagnostic);
      }
    }
  }

  protected void createMarkers(IResource resource, Diagnostic diagnostic, Diagnostic parentDiagnostic) throws CoreException
  {
    if (resource != null && resource.exists())
    {
      IMarker marker = resource.createMarker(getMarkerID());
      int severity = diagnostic.getSeverity();
      if (severity < Diagnostic.WARNING)
      {
        marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_INFO);
      }
      else if (severity < Diagnostic.ERROR)
      {
        marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_WARNING);
      }
      else
      {
        marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);
      }
          
      String message = composeMessage(diagnostic, parentDiagnostic);
      if (message != null)
      {
        marker.setAttribute(IMarker.MESSAGE, message);
      }
          
      adjustMarker(marker, diagnostic, parentDiagnostic);
    }
  }
  
  /**
   * Clients should override this method to update the marker associated with the diagnostic.
   * @param marker the marker to be updated.
   * @param diagnostic the diagnostic associated with the marker.
   * @param parentDiagnostic the parent of the diagnostic, if any.
   * @throws CoreException
   */
  protected void adjustMarker(IMarker marker, Diagnostic diagnostic, Diagnostic parentDiagnostic) throws CoreException
  {
  }  
      
  /**
   * Returns the message that will be used with the marker associated with the given diagnostic.
   * @param diagnostic the diagnostic.
   * @param parentDiagnostic the parent of the diagnostic, if any. 
   * @return the message that will be used with the marker associated with the given diagnostic.
   */
  protected String composeMessage(Diagnostic diagnostic, Diagnostic parentDiagnostic)
  {
    return diagnostic.getMessage();
  }

  /**
   * Returns whether the a maker with id equals to the return of {@link #getMarkerID()}
   * is available in the IResource computed from the specified object.
   * @param object
   * @return boolean
   */
  public boolean hasMarkers(Object object)
  {
    return hasMarkers(object, false, IResource.DEPTH_ZERO);
  }
  
  public boolean hasMarkers(Object object, boolean includeSubtypes, int depth)
  {
    return hasMarkers(getFile(object), includeSubtypes, depth);
  }

  protected boolean hasMarkers(IResource resource, boolean includeSubtypes, int depth)
  {
    if (resource != null && resource.exists())
    {
      try
      {
        IMarker[] markers = resource.findMarkers(getMarkerID(), includeSubtypes, depth);
        return markers.length > 0;
      }
      catch (CoreException e)
      {
      }
    }
    return false;
  }

  /**
   * Deletes a maker with id equals to the return of {@link #getMarkerID()}
   * from the IResource computed from the specified object.
   * @param object
   * @return boolean
   */  
  public void deleteMarkers(Object object)
  {
    deleteMarkers(object, false, IResource.DEPTH_ZERO);
  }

  public void deleteMarkers(Object object, boolean includeSubtypes, int depth)
  {
    deleteMarkers(getFile(object), includeSubtypes, depth);
  }
  
  protected void deleteMarkers(IResource resource, boolean includeSubtypes, int depth)
  {
    if (resource != null && resource.exists())
    {
      try
      {
        resource.deleteMarkers(getMarkerID(), includeSubtypes, depth);
      }
      catch (CoreException e)
      {
        CommonUIPlugin.INSTANCE.log(e);
      }
    }
  }  
  
  public IEditorInput getEditorInput(Object object)
  {
    IFile file = getFile(object);
    if (file != null)
    {
      return new FileEditorInput(file);
    }
    else
    {
      return null;
    }
  }
}