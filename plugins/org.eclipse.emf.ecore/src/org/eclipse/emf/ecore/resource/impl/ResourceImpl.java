/**
 * Copyright (c) 2002-2013 IBM Corporation and others.
 * All rights reserved.   This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   IBM - Initial API and implementation
 */
package org.eclipse.emf.ecore.resource.impl;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.eclipse.emf.common.notify.Adapter;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.notify.NotificationChain;
import org.eclipse.emf.common.notify.impl.AdapterImpl;
import org.eclipse.emf.common.notify.impl.NotificationChainImpl;
import org.eclipse.emf.common.notify.impl.NotificationImpl;
import org.eclipse.emf.common.notify.impl.NotifierImpl;
import org.eclipse.emf.common.notify.impl.NotifyingListImpl;
import org.eclipse.emf.common.util.AbstractTreeIterator;
import org.eclipse.emf.common.util.BasicEList;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.SegmentSequence;
import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.common.util.WrappedException;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.InternalEObject;
import org.eclipse.emf.ecore.resource.ContentHandler;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.URIConverter;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.ecore.util.InternalEList;
import org.eclipse.emf.ecore.util.NotifyingInternalEListImpl;
import org.eclipse.emf.ecore.util.EcoreUtil.ContentTreeIterator;
import org.eclipse.emf.ecore.util.EcoreUtil.ProperContentIterator;


/**
 * A highly extensible resource implementation.
 * <p>
 * The following configuration and control mechanisms are provided:
 * <ul>
 *   <li><b>Serialization</b></li>
 *   <ul>
 *     <li>{@link #doSave(OutputStream, Map)}</li>
 *     <li>{@link #doLoad(InputStream, Map)}</li>
 *     <li>{@link #doUnload}</li>
 *   </ul>
 *   <li><b>Root URI Fragment</b></li>
 *   <ul>
 *     <li>{@link #getURIFragmentRootSegment(EObject)}</li>
 *     <li>{@link #getEObjectForURIFragmentRootSegment(String)}</li>
 *   </ul>
 *   <li><b>Containment Changes</b></li>
 *   <ul>
 *     <li>{@link #attached(EObject)}</li>
 *     <li>{@link #detached(EObject)}</li>
 *     <li>{@link #unloaded(InternalEObject)}</li>
 *   </ul>
 *   <li><b>ZIP</b></li>
 *   <ul>
 *     <li>{@link #useZip}</li>
 *     <li>{@link #newContentZipEntry}</li>
 *     <li>{@link #isContentZipEntry(ZipEntry)}</li>
 *   </ul>
 *   <li><b>URI Conversion</b></li>
 *   <ul>
 *     <li>{@link #getURIConverter}</li>
 *   </ul>
 *   <li><b>Modification</b></li>
 *   <ul>
 *     <li>{@link #createModificationTrackingAdapter()}</li>
 *   </ul>
 * </ul>
 * </p>
 */
public class ResourceImpl extends NotifierImpl implements Resource, Resource.Internal
{
  /**
   * The default URI converter when there is no resource set.
   */
  private static URIConverter defaultURIConverter;

  /**
   * Returns the default URI converter that's used when there is no resource set.
   * @return the default URI converter.
   * @see #getURIConverter
   */
  protected static URIConverter getDefaultURIConverter()
  {
    if (defaultURIConverter == null)
    {
      defaultURIConverter = new ExtensibleURIConverterImpl();
    }
    return defaultURIConverter;
  }

  /**
   * Merges 2 maps, without changing any of them.  If map2 and map1
   * have the same key for an entry, map1's value will be the one in
   * the merged map.
   */
  protected static Map<?, ?> mergeMaps(Map<?, ?> map1, Map<?, ?> map2)
  {
    if (map1 == null || map1.isEmpty())
    {
      return map2;
    }
    else if (map2 == null || map2.isEmpty())
    {
      return map1;
    }
    else
    {
      return new ExtensibleURIConverterImpl.OptionsMap(map1, map2);
    }
  }

  /**
   * The storage for the default save options.
   */
  protected Map<Object, Object> defaultSaveOptions;

  /**
   * The storage for the default load options.
   */
  protected Map<Object, Object> defaultLoadOptions;

  /**
   * The storage for the default delete options.
   */
  protected Map<Object, Object> defaultDeleteOptions;

  /**
   * The containing resource set.
   * @see #getResourceSet
   */
  protected ResourceSet resourceSet;

  /**
   * The URI.
   * @see #getURI
   */
  protected URI uri;

  /**
   * The time stamp.
   * @see #getTimeStamp
   */
  protected long timeStamp;

  /**
   * The contents.
   * @see #getContents
   */
  protected ContentsEList<EObject> contents;

  /**
   * The errors.
   * @see #getErrors
   */
  protected EList<Diagnostic> errors;

  /**
   * The warnings.
   * @see #getErrors
   */
  protected EList<Diagnostic> warnings;

  /**
   * The modified flag.
   * @see #isModified
   */
  protected boolean isModified;

  /**
   * The loaded flag.
   * @see #isLoaded
   */
  protected boolean isLoaded;

  /**
   * The loading flag.
   * @see #isLoading
   */
  protected boolean isLoading;

  /**
   * A copy of the {@link #contents contents} list while the contents are being {@link #unload() unloaded}.
   * I.e., if this is not <code>null</code>, then the resource is in the process of unloading.
   * @see #unload()
   */
  protected List<EObject> unloadingContents;

  /**
   * The modification tracking adapter.
   * @see #isTrackingModification
   * @see #attached(EObject)
   * @see #detached(EObject)
   */
  protected Adapter modificationTrackingAdapter;

  /**
   * A map to retrieve the EObject based on the value of its ID feature.
   * @see #setIntrinsicIDToEObjectMap(Map)
   */
  protected Map<String, EObject> intrinsicIDToEObjectMap;

  /**
   * Creates a empty instance.
   */
  public ResourceImpl()
  {
    super();
  }

  /**
   * Creates an instance with the given URI.
   * @param uri the URI.
   */
  public ResourceImpl(URI uri)
  {
    this();
    this.uri = uri;
  }

  /*
   * Javadoc copied from interface.
   */
  public ResourceSet getResourceSet()
  {
    return resourceSet;
  }

  /**
   * Sets the new containing resource set, and removes the resource from a previous containing resource set, if necessary.
   * @param resourceSet the new containing resource set.
   * @param notifications the accumulating notifications.
   * @return notification of the change.
   */
  public NotificationChain basicSetResourceSet(ResourceSet resourceSet, NotificationChain notifications)
  {
    ResourceSet oldResourceSet = this.resourceSet;
    if (oldResourceSet != null)
    {
      notifications = ((InternalEList<Resource>)oldResourceSet.getResources()).basicRemove(this, notifications);
    }

    this.resourceSet = resourceSet;

    if (eNotificationRequired())
    {
      if (notifications == null)
      {
        notifications = new NotificationChainImpl(2);
      }
      notifications.add
        (new NotificationImpl(Notification.SET, oldResourceSet, resourceSet)
         {
           @Override
           public Object getNotifier()
           {
             return ResourceImpl.this;
           }

           @Override
           public int getFeatureID(Class<?> expectedClass)
           {
             return RESOURCE__RESOURCE_SET;
           }
         });
    }

    return notifications;
  }

  /*
   * Javadoc copied from interface.
   */
  public URI getURI()
  {
    return uri;
  }

  /*
   * Javadoc copied from interface.
   */
  public void setURI(URI uri)
  {
    URI oldURI = this.uri;
    this.uri = uri;
    if (eNotificationRequired())
    {
      Notification notification =
        new NotificationImpl(Notification.SET, oldURI, uri)
        {
          @Override
          public Object getNotifier()
          {
            return ResourceImpl.this;
          }

          @Override
          public int getFeatureID(Class<?> expectedClass)
          {
            return RESOURCE__URI;
          }
        };
      eNotify(notification);
    }
  }

  public long getTimeStamp()
  {
    return timeStamp;
  }

  public void setTimeStamp(long timeStamp)
  {
    long oldTimeStamp = this.timeStamp;
    this.timeStamp = timeStamp;
    if (eNotificationRequired())
    {
      Notification notification =
        new NotificationImpl(Notification.SET, oldTimeStamp, timeStamp)
        {
          @Override
          public Object getNotifier()
          {
            return ResourceImpl.this;
          }

          @Override
          public int getFeatureID(Class<?> expectedClass)
          {
            return RESOURCE__TIME_STAMP;
          }
        };
      eNotify(notification);
    }
  }

  /**
   * A notifying list implementation for supporting {@link Resource#getContents}.
   */
  protected class ContentsEList<E extends Object & EObject> extends NotifyingInternalEListImpl<E> implements InternalEList<E>
  {
    private static final long serialVersionUID = 1L;

    @Override
    public Object getNotifier()
    {
      return ResourceImpl.this;
    }

    @Override
    public int getFeatureID()
    {
      return RESOURCE__CONTENTS;
    }

    @Override
    protected boolean isNotificationRequired()
    {
      return ResourceImpl.this.eNotificationRequired();
    }

    @Override
    protected boolean useEquals()
    {
      return false;
    }

    @Override
    protected boolean hasInverse()
    {
      return true;
    }

    @Override
    protected boolean isUnique()
    {
      return true;
    }

    @Override
    public NotificationChain inverseAdd(E object, NotificationChain notifications)
    {
      InternalEObject eObject = (InternalEObject)object;
      notifications = eObject.eSetResource(ResourceImpl.this, notifications);
      ResourceImpl.this.attached(eObject);
      return notifications;
    }

    @Override
    public NotificationChain inverseRemove(E object, NotificationChain notifications)
    {
      InternalEObject eObject = (InternalEObject)object;
      if (ResourceImpl.this.isLoaded || unloadingContents != null)
      {
        ResourceImpl.this.detached(eObject);
      }
      return eObject.eSetResource(null, notifications);
    }

    @Override
    protected Object [] newData(int capacity)
    {
      return new EObject [capacity];
    }

    @Override
    protected void didAdd(int index, E object)
    {
      super.didAdd(index, object);
      if (index == size - 1)
      {
        loaded();
      }
      modified();
    }

    @Override
    protected void didRemove(int index, E object)
    {
      super.didRemove(index, object);
      modified();
    }

    @Override
    protected void didSet(int index, E newObject, E oldObject)
    {
      super.didSet(index, newObject, oldObject);
      modified();
    }

    @Override
    protected void didClear(int oldSize, Object [] oldData)
    {
      if (oldSize == 0)
      {
        loaded();
      }
      else
      {
        super.didClear(oldSize, oldData);
      }
    }

    protected void loaded()
    {
      if (!ResourceImpl.this.isLoaded())
      {
        Notification notification = ResourceImpl.this.setLoaded(true);
        if (notification != null)
        {
          ResourceImpl.this.eNotify(notification);
        }
      }
    }

    protected void modified()
    {
      if (isTrackingModification())
      {
        setModified(true);
      }
    }

    @Override
    public boolean contains(Object object)
    {
      return size <= 4 ? super.contains(object) : object instanceof InternalEObject && ((InternalEObject)object).eDirectResource() == ResourceImpl.this;
    }
  }

  /*
   * Javadoc copied from interface.
   */
  public EList<EObject> getContents()
  {
    if (contents == null)
    {
      contents = new ContentsEList<EObject>();
    }
    return contents;
  }

  /*
   * Javadoc copied from interface.
   */
  public TreeIterator<EObject> getAllContents()
  {
    return
      new AbstractTreeIterator<EObject>(this, false)
      {
        private static final long serialVersionUID = 1L;

        @Override
        public Iterator<EObject> getChildren(Object object)
        {
          return object == ResourceImpl.this ? ResourceImpl.this.getContents().iterator() : ((EObject)object).eContents().iterator();
        }
      };
  }

  protected TreeIterator<EObject> getAllProperContents(EObject eObject)
  {
    return EcoreUtil.getAllProperContents(eObject, false);
  }

  protected TreeIterator<EObject> getAllProperContents(List<EObject> contents)
  {
    return
      new ContentTreeIterator<EObject>(contents, false)
      {
        private static final long serialVersionUID = 1L;

        @SuppressWarnings("unchecked")
        @Override
        public Iterator<EObject> getChildren(Object object)
        {
          return
            object == this.object ?
              ((List<EObject>)object).iterator() :
              new ProperContentIterator<EObject>(((EObject)object));
        }
      };
  }

  /*
   * Javadoc copied from interface.
   */
  public EList<Diagnostic> getErrors()
  {
    if (errors == null)
    {
      errors =
        new NotifyingListImpl<Diagnostic>()
        {
          private static final long serialVersionUID = 1L;

          @Override
          protected boolean isNotificationRequired()
          {
             return ResourceImpl.this.eNotificationRequired();
          }

          @Override
          public Object getNotifier()
          {
            return ResourceImpl.this;
          }

          @Override
          public int getFeatureID()
          {
            return RESOURCE__ERRORS;
          }
        };
    }
    return errors;
  }

  /*
   * Javadoc copied from interface.
   */
  public EList<Diagnostic> getWarnings()
  {
    if (warnings == null)
    {
      warnings =
        new NotifyingListImpl<Diagnostic>()
        {
          private static final long serialVersionUID = 1L;

          @Override
          protected boolean isNotificationRequired()
          {
             return ResourceImpl.this.eNotificationRequired();
          }

          @Override
          public Object getNotifier()
          {
            return ResourceImpl.this;
          }

          @Override
          public int getFeatureID()
          {
            return RESOURCE__WARNINGS;
          }
        };
    }
    return warnings;
  }

  /**
   * Returns whether contents will be compressed.
   * This implementation returns <code>false</code>.
   * When this returns <code>true</code>,
   * {@link #save(OutputStream, Map)} and {@link #load(InputStream, Map)}
   * will zip compress and decompress contents.
   * @return whether contents will be compressed.
   * @see #newContentZipEntry
   * @see #isContentZipEntry(ZipEntry)
   */
  protected boolean useZip()
  {
    return false;
  }


  /**
   * Returns the URI fragment root segment for reaching the given direct content object.
   * This default implementation returns the position of the object, if there is more than one object,
   * otherwise, the empty string.
   * As a result, the URI fragment for a single root object will be <code>"/"</code>.
   * @return the URI fragment root segment for reaching the given direct content object.
   */
  protected String getURIFragmentRootSegment(EObject eObject)
  {
    List<EObject> contents = unloadingContents != null ? unloadingContents : getContents();
    return contents.size() > 1 ?
      Integer.toString(contents.indexOf(eObject)) :
      "";
  }

  /*
   * Javadoc copied from interface.
   */
  public String getURIFragment(EObject eObject)
  {
    String id = EcoreUtil.getID(eObject);
    if (id != null)
    {
      return id;
    }
    else
    {
      InternalEObject internalEObject = (InternalEObject)eObject;
      if (internalEObject.eDirectResource() == this || unloadingContents != null && unloadingContents.contains(internalEObject))
      {
        return "/" + getURIFragmentRootSegment(eObject);
      }
      else
      {
        SegmentSequence.Builder builder = SegmentSequence.newBuilder("/");

        boolean supportIDRelativeURIFragmentPaths = supportIDRelativeURIFragmentPaths();
        boolean isContained = false;
        for (InternalEObject container = internalEObject.eInternalContainer(); container != null; container = internalEObject.eInternalContainer())
        {
          // If we've not already found an ID, which will be the last segment, then continue building segments.
          //
          if (id == null)
          {
            builder.append(container.eURIFragmentSegment(internalEObject.eContainingFeature(), internalEObject));

            // We will stop appending segments but will continue the loop for proper isContained checking if there is an ID.
            //
            if (supportIDRelativeURIFragmentPaths)
            {
              id = getIDForEObject(container);
            }
          }

          internalEObject = container;
          if (container.eDirectResource() == this || unloadingContents != null && unloadingContents.contains(container))
          {
            isContained = true;
            break;
          }
        }
  
        if (!isContained)
        {
          return "/-1";
        }
  
        builder.append(id != null ? "?" + id : getURIFragmentRootSegment(internalEObject));
        builder.append("");
        builder.reverse();

        // Note that we convert it to a segment sequence because the most common use case is that callers of this method will call URI.appendFragment.
        // By creating the segment sequence here, we ensure that it's found in the cache.
        //
        return builder.toSegmentSequence().toString();
      }
    }
  }

  /**
   * Returns whether {@link #getURIFragment(EObject)} should support ID-relative URI fragment segments.
   * Normally that method returns either the ID of the object,
   * or a fragment path relative to the root object.
   * When this is enabled, the fragment path construction stops at the first {@link #getIDForEObject(EObject) object with an ID}
   * to construct a path of to form {@code /?<id>/...}
   * @since 2.14
   */
  protected boolean supportIDRelativeURIFragmentPaths()
  {
    return false;
  }

  /**
   * Returns the ID for the given EObject such that {@link #getEObjectByID(String)} would return this same object.
   * It is used by {@link #getURIFragment(EObject)}, but only if {@link #supportIDRelativeURIFragmentPaths()} is {@code true}.
   *
   * @since 2.14
   */
  protected String getIDForEObject(EObject eObject)
  {
    return EcoreUtil.getID(eObject);
  }

  /**
   * Returns the object associated with the URI fragment root segment.
   * This default implementation uses the position of the object;
   * an empty string is the same as <code>"0"</code>.
   * @return the object associated with the URI fragment root segment.
   */
  protected EObject getEObjectForURIFragmentRootSegment(String uriFragmentRootSegment)
  {
    int position =  0;
    if (uriFragmentRootSegment.length() > 0)
    {
      if (uriFragmentRootSegment.charAt(0) == '?')
      {
        return getEObjectByID(uriFragmentRootSegment.substring(1));
      }
      else
      {
        try
        {
          position = Integer.parseInt(uriFragmentRootSegment);
        }
        catch (NumberFormatException exception)
        {
          throw new WrappedException(exception);
        }
      }
    }

    List<EObject> contents = getContents();
    if (position < contents.size() && position >= 0)
    {
      return contents.get(position);
    }
    else
    {
      return null;
    }
  }

  /*
   * Javadoc copied from interface.
   */
  public EObject getEObject(String uriFragment)
  {
    int length = uriFragment.length();
    if (length > 0)
    {
      if (uriFragment.charAt(0) == '/')
      {
        return getEObject(SegmentSequence.create("/", uriFragment).subSegmentsList(1));
      }
      else if (uriFragment.charAt(length - 1) == '?')
      {
        int index = uriFragment.lastIndexOf('?', length - 2);
        if (index > 0)
        {
          uriFragment = uriFragment.substring(0, index);
        }
      }
    }

    return getEObjectByID(uriFragment);
  }

  /**
   * Returns the object based on the fragment path as a list of Strings.
   */
  protected EObject getEObject(List<String> uriFragmentPath)
  {
    int size = uriFragmentPath.size();
    EObject eObject = getEObjectForURIFragmentRootSegment(size == 0 ? "" : uriFragmentPath.get(0));
    for (int i = 1; i < size && eObject != null; ++i)
    {
      eObject = ((InternalEObject)eObject).eObjectForURIFragmentSegment(uriFragmentPath.get(i));
    }

    return eObject;
  }

  /**
   * Returns the map used to cache the EObject that is identified by the {@link #getEObjectByID(String) value}
   * of its ID feature.
   * @return the map used to cache the EObject that is identified by the value of its ID feature.
   * @see #setIntrinsicIDToEObjectMap
   */
  public Map<String, EObject> getIntrinsicIDToEObjectMap()
  {
    return intrinsicIDToEObjectMap;
  }

  /**
   * Sets the map used to cache the EObject identified by the value of its ID feature.
   * This cache is only activated if the map is not <code>null</code>.
   * The map will be lazily loaded by the {@link #getEObjectByID(String) getEObjectByID} method.
   * It is up to the client to clear the cache when it becomes invalid,
   * e.g., when the ID of a previously mapped EObject is changed.
   * @param intrinsicIDToEObjectMap the new map or <code>null</code>.
   * @see #getIntrinsicIDToEObjectMap
   */
  public void setIntrinsicIDToEObjectMap(Map<String, EObject> intrinsicIDToEObjectMap)
  {
    this.intrinsicIDToEObjectMap = intrinsicIDToEObjectMap;
  }


  /**
   * Returns the object based on the fragment as an ID.
   */
  protected EObject getEObjectByID(String id)
  {
    Map<String, EObject> map = getIntrinsicIDToEObjectMap();
    if (map != null)
    {
      EObject eObject = map.get(id);
      if (eObject != null)
      {
        return eObject;
      }
    }

    EObject result = null;
    for (TreeIterator<EObject> i = getAllProperContents(getContents()); i.hasNext(); )
    {
      EObject eObject = i.next();
      String eObjectId = EcoreUtil.getID(eObject);
      if (eObjectId != null)
      {
        if (map != null)
        {
          map.put(eObjectId, eObject);
        }

        if (eObjectId.equals(id))
        {
          result = eObject;
          if (map == null)
          {
            break;
          }
        }
      }
    }

    return result;
  }

  public void attached(EObject eObject)
  {
    if (isAttachedDetachedHelperRequired())
    {
      attachedHelper(eObject);
      for (TreeIterator<EObject> tree = getAllProperContents(eObject); tree.hasNext(); )
      {
        attachedHelper(tree.next());
      }
    }
  }

  protected boolean isAttachedDetachedHelperRequired()
  {
    return isTrackingModification() || getIntrinsicIDToEObjectMap() != null;
  }

  protected void attachedHelper(EObject eObject)
  {
    if (isTrackingModification())
    {
      eObject.eAdapters().add(modificationTrackingAdapter);
    }

    Map<String, EObject> map = getIntrinsicIDToEObjectMap();
    if (map != null)
    {
      String id = EcoreUtil.getID(eObject);
      if (id != null)
      {
        map.put(id, eObject);
      }
    }
  }


  /**
   * Adds modification tracking adapters to the object and it's content tree.
   * @param eObject the object.
   * @see #attached(EObject)
   * @deprecated since 2.1.0.  This method is not invoked anymore.  See
   * {@link #attachedHelper(EObject)}.
   */
  @Deprecated
  final protected void addModificationTrackingAdapters(EObject eObject)
  {
    // Do nothing.
  }

  public void detached(EObject eObject)
  {
    if (isAttachedDetachedHelperRequired())
    {
      detachedHelper(eObject);
      for (TreeIterator<EObject> tree = getAllProperContents(eObject); tree.hasNext(); )
      {
        detachedHelper(tree.next());
      }
    }
  }

  protected void detachedHelper(EObject eObject)
  {
    Map<String, EObject> map = getIntrinsicIDToEObjectMap();
    if (map != null)
    {
      String id = EcoreUtil.getID(eObject);
      if (id != null)
      {
        map.remove(id);
      }
    }

    if (isTrackingModification())
    {
      eObject.eAdapters().remove(modificationTrackingAdapter);
    }
  }

  /**
   * Removes modification tracking adapters to the object and it's content tree.
   * @param eObject the object.
   * @see #detached(EObject)
   * @deprecated since 2.1.0.  This method is not invoked anymore.  See
   * {@link #attachedHelper(EObject)}.
   */
  @Deprecated
  final protected void removeModificationTrackingAdapters(EObject eObject)
  {
    // Do nothing.
  }


  /**
   * Returns the URI converter.
   * This typically gets the {@link ResourceSet#getURIConverter converter}
   * from the {@link #getResourceSet containing} resource set,
   * but it calls {@link #getDefaultURIConverter} when there is no containing resource set.
   * @return the URI converter.
   */
  protected URIConverter getURIConverter()
  {
    return
      getResourceSet() == null ?
        getDefaultURIConverter() :
        getResourceSet().getURIConverter();
  }

  private static final Set<String> LINE_DELIMITER_REQUEST = Collections.singleton(ContentHandler.LINE_DELIMITER_PROPERTY);

  /*
   * Javadoc copied from interface.
   */
  public void save(Map<?, ?> options) throws IOException
  {
    Object saveOnlyIfChanged =
      options != null && options.containsKey(OPTION_SAVE_ONLY_IF_CHANGED) ?
        options.get(OPTION_SAVE_ONLY_IF_CHANGED) :
        defaultSaveOptions != null ?
          defaultSaveOptions.get(OPTION_SAVE_ONLY_IF_CHANGED) :
          null;

    String lineDelimiter =
      options != null && options.containsKey(OPTION_LINE_DELIMITER) ?
        (String)options.get(OPTION_LINE_DELIMITER) :
        defaultSaveOptions != null ?
          (String)defaultSaveOptions.get(OPTION_LINE_DELIMITER) :
          null;

    if (OPTION_LINE_DELIMITER_UNSPECIFIED.equals(lineDelimiter))
    {
      ExtensibleURIConverterImpl.OptionsMap effectiveOptions = new ExtensibleURIConverterImpl.OptionsMap(ContentHandler.OPTION_REQUESTED_PROPERTIES, LINE_DELIMITER_REQUEST, options, defaultSaveOptions);
      Map<String, ?> contentDescription = getURIConverter().contentDescription(uri, effectiveOptions);
      lineDelimiter = (String)contentDescription.get(ContentHandler.LINE_DELIMITER_PROPERTY);
      if (lineDelimiter != null)
      {
        options = new ExtensibleURIConverterImpl.OptionsMap(OPTION_LINE_DELIMITER, lineDelimiter, options, defaultSaveOptions);
      }
    }

    if (OPTION_SAVE_ONLY_IF_CHANGED_FILE_BUFFER.equals(saveOnlyIfChanged))
    {
      saveOnlyIfChangedWithFileBuffer(options);
    }
    else if (OPTION_SAVE_ONLY_IF_CHANGED_MEMORY_BUFFER.equals(saveOnlyIfChanged))
    {
      saveOnlyIfChangedWithMemoryBuffer(options);
    }
    else
    {
      Map<?, ?> response = options == null ? null : (Map<?, ?>)options.get(URIConverter.OPTION_RESPONSE);
      if (response == null)
      {
        response = new HashMap<Object, Object>();
      }
      URIConverter uriConverter = getURIConverter();
      ExtensibleURIConverterImpl.OptionsMap effectiveOptions = new ExtensibleURIConverterImpl.OptionsMap(URIConverter.OPTION_RESPONSE, response, options, defaultSaveOptions);
      OutputStream outputStream = uriConverter.createOutputStream(getURI(), effectiveOptions);
      try
      {
        save(outputStream, options);
      }
      finally
      {
        outputStream.close();
        handleSaveResponse(response, effectiveOptions);
      }
    }
  }

  /**
   * Handle the processing of the response after the stream has been closed during save.
   * @since 2.7
   */
  protected void handleSaveResponse(Map<?, ?> response, Map<?, ?> options)
  {
     Long timeStamp = (Long)response.get(URIConverter.RESPONSE_TIME_STAMP_PROPERTY);
     if (timeStamp != null)
     {
       setTimeStamp(timeStamp);
     }
     URI uri = (URI)response.get(URIConverter.RESPONSE_URI);
     if (uri != null)
     {
       setURI(uri);
     }
  }

  protected void saveOnlyIfChangedWithFileBuffer(Map<?, ?> options) throws IOException
  {
    File temporaryFile = File.createTempFile("ResourceSaveHelper", null);
    try
    {
      URI temporaryFileURI = URI.createFileURI(temporaryFile.getPath());
      URIConverter uriConverter = getURIConverter();
      OutputStream temporaryFileOutputStream = uriConverter.createOutputStream(temporaryFileURI, null);
      try
      {
        save(temporaryFileOutputStream, options);
      }
      finally
      {
        temporaryFileOutputStream.close();
      }

      boolean equal = true;
      InputStream oldContents = null;
      try
      {
        oldContents = getUnderlyingInputStream(uriConverter.createInputStream(getURI(), defaultLoadOptions), options);
      }
      catch (IOException exception)
      {
        equal = false;
      }
      byte [] newContentBuffer = new byte [4000];
      if (oldContents != null)
      {
        try
        {
          InputStream newContents = getUnderlyingInputStream(uriConverter.createInputStream(temporaryFileURI, null), options);
          try
          {
            byte [] oldContentBuffer = new byte [4000];
            LOOP:
            for (int oldLength = oldContents.read(oldContentBuffer), newLength = newContents.read(newContentBuffer);
                 (equal = oldLength == newLength) &&  oldLength > 0;
                 oldLength = oldContents.read(oldContentBuffer), newLength = newContents.read(newContentBuffer))
            {
              for (int i = 0; i < oldLength; ++i)
              {
                if (oldContentBuffer[i] != newContentBuffer[i])
                {
                  equal = false;
                  break LOOP;
                }
              }
            }
          }
          finally
          {
            newContents.close();
          }
        }
        finally
        {
          oldContents.close();
        }
      }

      if (!equal)
      {
        Map<?, ?> response = options == null ? null : (Map<?, ?>)options.get(URIConverter.OPTION_RESPONSE);
        if (response == null)
        {
          response = new HashMap<Object, Object>();
        }
        ExtensibleURIConverterImpl.OptionsMap effectiveOptions = new ExtensibleURIConverterImpl.OptionsMap(URIConverter.OPTION_RESPONSE, response, options, defaultSaveOptions);
        OutputStream newContents = uriConverter.createOutputStream(getURI(), effectiveOptions);
        try
        {
          InputStream temporaryFileContents = uriConverter.createInputStream(temporaryFileURI, null);
          try
          {
            for (int length = temporaryFileContents.read(newContentBuffer); length > 0; length = temporaryFileContents.read(newContentBuffer))
            {
              newContents.write(newContentBuffer, 0, length);
            }
          }
          finally
          {
            temporaryFileContents.close();
          }
        }
        finally
        {
          newContents.close();
          handleSaveResponse(response, effectiveOptions);
        }
      }
    }
    finally
    {
      temporaryFile.delete();
    }
  }

  protected void saveOnlyIfChangedWithMemoryBuffer(Map<?, ?> options) throws IOException
  {
    URIConverter uriConverter = getURIConverter();
    class MyByteArrayOutputStream extends ByteArrayOutputStream
    {
      public byte[] buffer()
      {
        return buf;
      }

      public int length()
      {
        return count;
      }
    }
    MyByteArrayOutputStream memoryBuffer = new MyByteArrayOutputStream();
    try
    {
      save(memoryBuffer, options);
    }
    finally
    {
      memoryBuffer.close();
    }

    byte [] newContentBuffer = memoryBuffer.buffer();
    int length = memoryBuffer.length();
    ByteArrayInputStream inputStream = new ByteArrayInputStream(newContentBuffer);
    InputStream underlyingInputStream = getUnderlyingInputStream(inputStream, options);
    byte [] underlyingNewContentBuffer;
    int underlyingLength;
    if (inputStream == underlyingInputStream)
    {
      underlyingNewContentBuffer = newContentBuffer;
      underlyingLength = length;
    }
    else
    {
      ByteArrayOutputStream bytes = new ByteArrayOutputStream();
      byte [] buffer = new byte[4000];
      for (int count = underlyingInputStream.read(buffer); count > 0; count = underlyingInputStream.read(buffer))
      {
        bytes.write(buffer, 0, count);
      }
      bytes.close();
      underlyingInputStream.close();
      underlyingNewContentBuffer = bytes.toByteArray();
      underlyingLength = underlyingNewContentBuffer.length;
    }

    boolean equal = true;
    InputStream oldContents = null;
    try
    {
      oldContents = getUnderlyingInputStream(uriConverter.createInputStream(getURI(), defaultLoadOptions), options);
    }
    catch (IOException exception)
    {
      equal = false;
    }
    if (oldContents != null)
    {
      try
      {
        byte [] oldContentBuffer = new byte [underlyingLength];
        int count = oldContents.read(oldContentBuffer); 
        while (count > 0 && count < underlyingLength)
        {
          int more = oldContents.read(oldContentBuffer, count, oldContentBuffer.length - count);
          if (more <= 0)
          {
            break;
          }
          else
          {
            count += more;
          }
        }
        if (count == underlyingLength && oldContents.read() == -1)
        {
          for (int i = 0; i < underlyingLength; ++i)
          {
            if (oldContentBuffer[i] != underlyingNewContentBuffer[i])
            {
              equal = false;
              break;
            }
          }
        }
        else
        {
          equal = false;
        }
      }
      finally
      {
        oldContents.close();
      }
    }

    if (!equal)
    {
      Map<?, ?> response = options == null ? null : (Map<?, ?>)options.get(URIConverter.OPTION_RESPONSE);
      if (response == null)
      {
        response = new HashMap<Object, Object>();
      }
      ExtensibleURIConverterImpl.OptionsMap effectiveOptions = new ExtensibleURIConverterImpl.OptionsMap(URIConverter.OPTION_RESPONSE, response, options, defaultSaveOptions);
      OutputStream newContents = uriConverter.createOutputStream(getURI(), effectiveOptions);
      try
      {
        newContents.write(newContentBuffer, 0, length);
      }
      finally
      {
        newContents.close();
        handleSaveResponse(response, effectiveOptions);
      }
    }
  }

  /*
   * Javadoc copied from interface.
   */
  public void load(Map<?, ?> options) throws IOException
  {
    if (!isLoaded)
    {
      URIConverter uriConverter = getURIConverter();
      Map<?, ?> response = options == null ? null : (Map<?, ?>)options.get(URIConverter.OPTION_RESPONSE);
      if (response == null)
      {
        response = new HashMap<Object, Object>();
      }

      // If an input stream can't be created, ensure that the resource is still considered loaded after the failure,
      // and do all the same processing we'd do if we actually were able to create a valid input stream.
      //
      InputStream inputStream = null;
      ExtensibleURIConverterImpl.OptionsMap effectiveOptions = new ExtensibleURIConverterImpl.OptionsMap(URIConverter.OPTION_RESPONSE, response, options, defaultLoadOptions);
      try
      {
        inputStream =
          uriConverter.createInputStream
            (getURI(),
             effectiveOptions);
      }
      catch (IOException exception)
      {
        Notification notification = setLoaded(true);
        isLoading = true;
        if (errors != null)
        {
          errors.clear();
        }
        if (warnings != null)
        {
          warnings.clear();
        }
        isLoading = false;
        if (notification != null)
        {
          eNotify(notification);
        }
        setModified(false);

        throw exception;
      }

      try
      {
        load(inputStream, options);
      }
      finally
      {
        inputStream.close();
        handleLoadResponse(response, effectiveOptions);
      }
    }
  }

  /**
   * Handle the processing of the response after the stream has been closed during load.
   * @since 2.7
   */
  protected void handleLoadResponse(Map<?, ?> response, Map<?, ?> options)
  {
    Long timeStamp = (Long)response.get(URIConverter.RESPONSE_TIME_STAMP_PROPERTY);
    if (timeStamp != null)
    {
      setTimeStamp(timeStamp);
    }
  }

  /**
   * Returns a new zip entry for {@link #save(OutputStream, Map) saving} the resource contents.
   * It is called by {@link #save(OutputStream, Map)} when writing {@link #useZip zipped} contents.
   * This implementation creates an entry called <code>ResourceContents</code>.
   * @return a new zip entry.
   * @see #isContentZipEntry(ZipEntry)
   */
  protected ZipEntry newContentZipEntry()
  {
    return new ZipEntry("ResourceContents");
  }

  /**
   * Returns the input stream for the zip entry, or the original input stream, as appropriate.
   */
  private InputStream getUnderlyingInputStream(InputStream inputStream, Map<?, ?> options) throws IOException
  {
    if (useZip() || (options != null && Boolean.TRUE.equals(options.get(Resource.OPTION_ZIP))))
    {
      ZipInputStream zipInputStream = new ZipInputStream(inputStream);
      while (zipInputStream.available() != 0)
      {
        ZipEntry zipEntry = zipInputStream.getNextEntry();
        if (isContentZipEntry(zipEntry))
        {
          return zipInputStream;
        }
      }
    }
    return inputStream;
  }

  /**
   * Saves the resource to the output stream using the specified options.
   * <p>
   * This implementation is <code>final</code>;
   * clients should override {@link #doSave doSave}.
   * </p>
   * @param options the save options.
   * @see #save(Map)
   * @see #doSave(OutputStream, Map)
   * @see #load(InputStream, Map)
   */
  public final void save(OutputStream outputStream, Map<?, ?> options) throws IOException
  {
    if (errors != null)
    {
      errors.clear();
    }

    if (warnings != null)
    {
      warnings.clear();
    }

    options = mergeMaps(options, defaultSaveOptions);
    ZipOutputStream zipOutputStream = null;
    if (useZip() || (options != null && Boolean.TRUE.equals(options.get(Resource.OPTION_ZIP))))
    {
      zipOutputStream =
        new ZipOutputStream(outputStream)
        {
          @Override
          public void finish() throws IOException
          {
            super.finish();
            def.end();
          }

          @Override
          public void flush()
          {
            // Do nothing.
          }

          @Override
          public void close() throws IOException
          {
            try
            {
              super.flush();
            }
            catch (IOException exception)
            {
              // Continue and try to close.
            }
            super.close();
          }
        };
      zipOutputStream.putNextEntry(newContentZipEntry());
      outputStream = zipOutputStream;
    }

    URIConverter.Cipher cipher = options != null ?
      (URIConverter.Cipher)options.get(Resource.OPTION_CIPHER) :
      null;

    if (cipher != null)
    {
      try
      {
        OutputStream encryptedOutputStream = cipher.encrypt(outputStream);
        outputStream = encryptedOutputStream;
      }
      catch (Exception e)
      {
        throw new IOWrappedException(e);
      }
    }

    doSave(outputStream, options);

    if (cipher != null)
    {
      try
      {
        cipher.finish(outputStream);
      }
      catch (Exception e)
      {
        throw new IOWrappedException(e);
      }
    }

    setModified(false);

    if (zipOutputStream != null)
    {
      zipOutputStream.finish();
    }
  }

  /**
   * Called to save the resource.
   * This implementation throws an exception;
   * clients must override it.
   * @param outputStream the stream
   * @param options the save options.
   * @exception UnsupportedOperationException
   */
  protected void doSave(OutputStream outputStream, Map<?, ?> options) throws IOException
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Returns whether the given entry is the content entry for this resource.
   * It is called by {@link #load(InputStream, Map)} when reading {@link #useZip zipped} contents.
   * This implementation return <code>true</code>;
   * i.e., the first entry will be read.
   * @return whether the given entry is the content entry for this resource.
   * @see #newContentZipEntry
   */
  protected boolean isContentZipEntry(ZipEntry zipEntry)
  {
    return true;
  }

  /*
   * Javadoc copied from interface.
   */
  public final void load(InputStream inputStream, Map<?, ?> options) throws IOException
  {
    if (!isLoaded)
    {
      Notification notification = setLoaded(true);
      isLoading = true;

      if (errors != null)
      {
        errors.clear();
      }

      if (warnings != null)
      {
        warnings.clear();
      }

      try
      {
        options = mergeMaps(options, defaultLoadOptions);
        inputStream = getUnderlyingInputStream(inputStream, options);
        URIConverter.Cipher cipher = options != null ?
          (URIConverter.Cipher)options.get(Resource.OPTION_CIPHER) :
          null;

        if (cipher != null)
        {
          try
          {
            inputStream = cipher.decrypt(inputStream);
          }
          catch (Exception e)
          {
            throw new IOWrappedException(e);
          }
        }

        doLoad(inputStream, options);

        if (cipher != null)
        {
          try
          {
            cipher.finish(inputStream);
          }
          catch (Exception e)
          {
            throw new IOWrappedException(e);
          }
        }
      }
      finally
      {
        isLoading = false;

        if (notification != null)
        {
          eNotify(notification);
        }

        setModified(false);
      }
    }
  }

  /**
   * Called to load the resource.
   * This implementation throws an exception;
   * clients must override it.
   * @param inputStream the stream
   * @param options the load options.
   * @exception UnsupportedOperationException
   */
  protected void doLoad(InputStream inputStream, Map<?, ?> options) throws IOException
  {
    throw new UnsupportedOperationException();
  }

  /*
   * Javadoc copied from interface.
   */
  public boolean isLoaded()
  {
    return isLoaded;
  }

  /*
   * Javadoc copied from interface.
   */
  public boolean isLoading()
  {
    return isLoading;
  }

  /**
   * Called when the object is unloaded.
   * This implementation
   * {@link InternalEObject#eSetProxyURI sets} the object to be a proxy
   * and clears the {@link #eAdapters adapters}.
   */
  protected void unloaded(InternalEObject internalEObject)
  {
    // Ensure that an unresolved containment proxy's URI isn't reset.
    //
    if (!internalEObject.eIsProxy())
    {
      internalEObject.eSetProxyURI(uri.appendFragment(getURIFragment(internalEObject)));
    }
    internalEObject.eAdapters().clear();
  }

  /**
   * Sets the load state as indicated, and returns a notification, if {@link org.eclipse.emf.common.notify.impl.BasicNotifierImpl#eNotificationRequired required}.
   * Clients are <b>not</b> expected to call this directly; it is managed by the implementation.
   * @param isLoaded whether the resource is loaded.
   * @return a notification.
   */
  protected Notification setLoaded(boolean isLoaded)
  {
    boolean oldIsLoaded = this.isLoaded;
    this.isLoaded = isLoaded;

    if (eNotificationRequired())
    {
      Notification notification =
        new NotificationImpl(Notification.SET, oldIsLoaded, isLoaded)
        {
          @Override
          public Object getNotifier()
          {
            return ResourceImpl.this;
          }

          @Override
          public int getFeatureID(Class<?> expectedClass)
          {
            return RESOURCE__IS_LOADED;
          }
        };
      return notification;
    }
    else
    {
      return null;
    }
  }

  /**
   * This implementation returns a copy of the {@link #getContents() contents}.
   * It is called by {@link #unload()} to initialize the value of {@link #unloadingContents}.
   * Clients populating the resource's contents on-demand
   * can override this implementation to return an empty list
   * when the resource's contents have not been accessed before the request to unload.
   * @since 2.11
   */
  protected List<EObject> getUnloadingContents()
  {
    return new BasicEList.FastCompare<EObject>(getContents());
  }

  /**
   * Does all the work of unloading the resource.
   * It calls {@link #unloaded unloaded} for each object it the content {@link #getAllContents tree},
   * and clears the {@link #getContents contents}, {@link #getErrors errors}, and {@link #getWarnings warnings}.
   */
  protected void doUnload()
  {
    Iterator<EObject> allContents = getAllProperContents(unloadingContents);

    // This guard is needed to ensure that clear doesn't make the resource become loaded.
    //
    if (!getContents().isEmpty())
    {
      getContents().clear();
    }
    getErrors().clear();
    getWarnings().clear();

    while (allContents.hasNext())
    {
      unloaded((InternalEObject)allContents.next());
    }
  }

  /*
   * Javadoc copied from interface.
   */
  public final void unload()
  {
    if (isLoaded)
    {
      unloadingContents = getUnloadingContents();
      Notification notification = setLoaded(false);
      try
      {
        doUnload();
      }
      finally
      {
        unloadingContents = null;
        if (notification != null)
        {
          eNotify(notification);
        }
        setTimeStamp(URIConverter.NULL_TIME_STAMP);
      }
    }
  }

  public void delete(Map<?, ?> options) throws IOException
  {
    getURIConverter().delete(getURI(), mergeMaps(options, defaultDeleteOptions));
    unload();
    ResourceSet resourceSet = getResourceSet();
    if (resourceSet != null)
    {
      resourceSet.getResources().remove(this);
    }
  }

  /**
   * An adapter implementation for tracking resource modification.
   */
  protected class ModificationTrackingAdapter extends AdapterImpl
  {
    @Override
    public void notifyChanged(Notification notification)
    {
      if (!notification.isTouch())
      {
        setModified(true);
      }
    }
  }

  /*
   * Javadoc copied from interface.
   */
  public boolean isTrackingModification()
  {
    return modificationTrackingAdapter != null;
  }

  /*
   * Javadoc copied from interface.
   */
  public void setTrackingModification(boolean isTrackingModification)
  {
    boolean oldIsTrackingModification = modificationTrackingAdapter != null;

    if (oldIsTrackingModification != isTrackingModification)
    {
      if (isTrackingModification)
      {
        modificationTrackingAdapter = createModificationTrackingAdapter();

        for (TreeIterator<EObject> i = getAllProperContents(getContents()); i.hasNext(); )
        {
          EObject eObject = i.next();
          eObject.eAdapters().add(modificationTrackingAdapter);
        }
      }
      else
      {
        Adapter oldModificationTrackingAdapter = modificationTrackingAdapter;
        modificationTrackingAdapter = null;

        for (TreeIterator<EObject> i = getAllProperContents(getContents()); i.hasNext(); )
        {
          EObject eObject = i.next();
          eObject.eAdapters().remove(oldModificationTrackingAdapter);
        }
      }
    }

    if (eNotificationRequired())
    {
      Notification notification =
        new NotificationImpl(Notification.SET, oldIsTrackingModification, isTrackingModification)
        {
          @Override
          public Object getNotifier()
          {
            return ResourceImpl.this;
          }

          @Override
          public int getFeatureID(Class<?> expectedClass)
          {
            return RESOURCE__IS_TRACKING_MODIFICATION;
          }
        };
      eNotify(notification);
    }
  }


  /**
   * Creates a modification tracking adapter.
   * This implementation creates a {@link ResourceImpl.ModificationTrackingAdapter}.
   * Clients may override this to any adapter.
   * @see #modificationTrackingAdapter
   * @see #isTrackingModification
   */
  protected Adapter createModificationTrackingAdapter()
  {
    return new ModificationTrackingAdapter();
  }

  /*
   * Javadoc copied from interface.
   */
  public boolean isModified()
  {
    return isModified;
  }

  /*
   * Javadoc copied from interface.
   */
  public void setModified(boolean isModified)
  {
    boolean oldIsModified = this.isModified;
    this.isModified = isModified;
    if (eNotificationRequired())
    {
      Notification notification =
        new NotificationImpl(Notification.SET, oldIsModified, isModified)
        {
          @Override
          public Object getNotifier()
          {
            return ResourceImpl.this;
          }

          @Override
          public int getFeatureID(Class<?> expectedClass)
          {
            return RESOURCE__IS_MODIFIED;
          }
        };
      eNotify(notification);
    }
  }

  /**
   * If an implementation uses IDs and stores the IDs as part of the resource
   * rather than as objects, this method should return a string representation of
   * the ID to object mapping, which might be implemented as a Java Map.
   * @return a string representation of the ID to object mapping
   */
  public String toKeyString()
  {
    StringBuffer result = new StringBuffer("Key type: ");
    result.append(getClass().toString());
    return result.toString();
  }

  @Override
  public String toString()
  {
    return
      getClass().getName() + '@' + Integer.toHexString(hashCode()) +
        " uri='" + uri + "'";
  }
}
