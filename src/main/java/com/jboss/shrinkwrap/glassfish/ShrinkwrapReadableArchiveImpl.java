/*
 * JBoss, Home of Professional Open Source
 * Copyright 2009, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jboss.shrinkwrap.glassfish;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map.Entry;
import java.util.jar.Manifest;

import org.glassfish.api.deployment.archive.ReadableArchive;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ArchivePath;
import org.jboss.shrinkwrap.api.ArchivePaths;
import org.jboss.shrinkwrap.api.Asset;
import org.jboss.shrinkwrap.impl.base.AssignableBase;
import org.jboss.shrinkwrap.impl.base.Validate;
import org.jboss.shrinkwrap.impl.base.asset.ArchiveAsset;
import org.jboss.shrinkwrap.impl.base.asset.DirectoryAsset;
import org.jboss.shrinkwrap.impl.base.io.IOUtil;

/**
 * ShrinkWrap extension to support GlassFishs {@link ReadableArchive}
 * backed by an {@link Archive}
 *
 * @author <a href="mailto:aslak@conduct.no">Aslak Knutsen</a>
 * @version $Revision: $
 */
public class ShrinkwrapReadableArchiveImpl extends AssignableBase implements ShrinkwrapReadableArchive
{
   private final Archive<?> archive;

   /**
    * @param archive
    */
   public ShrinkwrapReadableArchiveImpl(final Archive<?> archive)
   {
      Validate.notNull(archive, "Archive must be specified");
      this.archive = archive;
   }

   /**
    * {@inheritDoc}
    * @see org.jboss.shrinkwrap.impl.base.AssignableBase#getArchive()
    */
   @Override
   protected Archive<?> getArchive()
   {
      return archive;
   }

   /**
    * A ShrinkWrap archive can not be deleted. 
    * 
    * @return This always returns false.
    */
   @Override
   public boolean delete()
   {
      return false;
   }

   /**
    * A ShrinkWrap archive can never not-exist. 
    * 
    * @return This always returns true.
    */
   @Override
   public boolean exists()
   {
      return true;
   }

   /**
    * {@inheritDoc}
    * @see org.glassfish.api.deployment.archive.ReadableArchive#exists(java.lang.String)
    */
   @Override
   public boolean exists(String path) throws IOException
   {
      return archive.contains(ArchivePaths.create(path));
   }

   /**
    * {@inheritDoc}
    * @see org.glassfish.api.deployment.archive.ReadableArchive#getEntry(java.lang.String)
    */
   @Override
   public InputStream getEntry(String path) throws IOException
   {
      return archive.get(ArchivePaths.create(path)).openStream();
   }

   /**
    * {@inheritDoc}
    * @see org.glassfish.api.deployment.archive.ReadableArchive#getEntrySize(java.lang.String)
    */
   // TODO: ShrinkWrap have not know the size of a Asset. Hacking figure it out runtime. 
   // Needed by GlassFish for buff sizes.
   @Override
   public long getEntrySize(String path)
   {
      Asset asset = archive.get(ArchivePaths.create(path));
      ByteArrayOutputStream output = new ByteArrayOutputStream();
      try
      {
         IOUtil.copyWithClose(asset.openStream(), output);
         return output.toByteArray().length;
      }
      catch (IOException e)
      {
         throw new RuntimeException(e);
      }
   }

   /**
    * ShrinkWrap does not have a bi-directional relationship between parent-child. 
    * 
    * @return This always return null.
    */
   @Override
   public ReadableArchive getParentArchive()
   {
      return null;
   }

   /**
    * ShrinkWrap does not have a bi-directional relationship between parent-child.
    * 
    *  This does nothing.
    */
   @Override
   public void setParentArchive(ReadableArchive arg0)
   {
   }

   /**
    * {@inheritDoc}
    * @see org.glassfish.api.deployment.archive.ReadableArchive#getSubArchive(java.lang.String)
    */
   // TODO: We should support non ShrinkWrap nested Archives as well. ie: external jar files.
   @Override
   public ReadableArchive getSubArchive(String path) throws IOException
   {
      Asset archiveAsset = archive.get(ArchivePaths.create(path));
      if (archiveAsset instanceof ArchiveAsset)
      {
         return ((ArchiveAsset) archiveAsset).getArchive().as(ShrinkwrapReadableArchive.class);
      }
      throw new IOException(path + " not a Archive");
   }

   /* (non-Javadoc)
    * @see org.glassfish.api.deployment.archive.ReadableArchive#open(java.net.URI)
    */
   @Override
   public void open(URI arg0) throws IOException
   {
   }

   /**
    * A ShrinkWrap archive can not be renamed. 
    * 
    * @return This always return false.
    */
   @Override
   public boolean renameTo(String arg0)
   {
      return false;
   }

   /* (non-Javadoc)
    * @see org.glassfish.api.deployment.archive.Archive#close()
    */
   @Override
   public void close() throws IOException
   {
   }

   /* (non-Javadoc)
    * @see org.glassfish.api.deployment.archive.Archive#entries()
    */
   @Override
   public Enumeration<String> entries()
   {
      List<String> entries = new ArrayList<String>();

      for (Entry<ArchivePath, Asset> entry : archive.getContent().entrySet())
      {
         entries.add(entry.getKey().get());
      }
      return Collections.enumeration(entries);
   }

   /* (non-Javadoc)
    * @see org.glassfish.api.deployment.archive.Archive#entries(java.lang.String)
    */
   @Override
   public Enumeration<String> entries(String path)
   {
      List<String> entries = new ArrayList<String>();

      for (Entry<ArchivePath, Asset> entry : archive.getContent().entrySet())
      {
         if (entry.getKey().get().startsWith(path))
         {
            entries.add(entry.getKey().get());
         }
      }
      return Collections.enumeration(entries);
   }

   /**
    * ShrinkWrap does not track Asset sizes.
    * 
    * @return This always return -1
    */
   @Override
   public long getArchiveSize() throws SecurityException
   {
      return -1;
   }

   /* (non-Javadoc)
    * @see org.glassfish.api.deployment.archive.Archive#getDirectories()
    */
   @Override
   public Collection<String> getDirectories() throws IOException
   {
      List<String> entries = new ArrayList<String>();

      for (Entry<ArchivePath, Asset> entry : archive.getContent().entrySet())
      {
         if (entry.getValue() == DirectoryAsset.INSTANCE)
         {
            entries.add(entry.getKey().get());
         }
      }
      return entries;
   }

   /* (non-Javadoc)
    * @see org.glassfish.api.deployment.archive.Archive#getManifest()
    */
   @Override
   public Manifest getManifest() throws IOException
   {
      ArchivePath manifestPath = ArchivePaths.create("META-INF/MANIFEST.MF");
      if (archive.contains(manifestPath))
      {
         return new Manifest(archive.get(manifestPath).openStream());
      }
      return null;
   }

   /* (non-Javadoc)
    * @see org.glassfish.api.deployment.archive.Archive#getName()
    */
   @Override
   public String getName()
   {
      return archive.getName();
   }

   /* (non-Javadoc)
    * @see org.glassfish.api.deployment.archive.Archive#getURI()
    */
   @Override
   public URI getURI()
   {
      try
      {
         return new URI("archive://" + archive.getName());
      }
      catch (URISyntaxException e)
      {
         throw new RuntimeException(e);
      }
   }

   /* (non-Javadoc)
    * @see org.glassfish.api.deployment.archive.Archive#isDirectory(java.lang.String)
    */
   @Override
   public boolean isDirectory(String path)
   {
      return archive.get(ArchivePaths.create(path)) == DirectoryAsset.INSTANCE;
   }
}