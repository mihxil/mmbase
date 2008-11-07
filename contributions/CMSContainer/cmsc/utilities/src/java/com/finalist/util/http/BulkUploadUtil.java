/*
 * 
 * This software is OSI Certified Open Source Software. OSI Certified is a certification mark of the Open Source
 * Initiative.
 * 
 * The license (Mozilla version 1.0) can be read at the MMBase site. See http://www.MMBase.org/license
 */
package com.finalist.util.http;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.upload.FormFile;
import org.mmbase.bridge.Cloud;
import org.mmbase.bridge.Node;
import org.mmbase.bridge.NodeManager;

import com.finalist.cmsc.mmbase.RelationUtil;
import com.finalist.cmsc.util.UploadUtil;
import com.finalist.cmsc.util.UploadUtil.BinaryData;

public class BulkUploadUtil {

   private static final Log log = LogFactory.getLog(BulkUploadUtil.class);

   private static final int MAXSIZE = 256 * 1024 * 1024;

   private static final String CONFIGURATION_RESOURCE_NAME = "/com/finalist/util/http/util.properties";

   private static final String ZIP_MIME_TYPES[] = new String[] { "application/x-zip-compressed", "application/zip",
         "application/x-zip" };

   private static Set<String> supportedImages;

   private static void initSupportedImages() {
      supportedImages = new HashSet<String>();
      Properties properties = new Properties();
      String images = ".bmp,.jpg,.jpeg,.gif,.png,.svg,.tiff,.tif";
      try {
         properties.load(BulkUploadUtil.class.getResourceAsStream(CONFIGURATION_RESOURCE_NAME));
         images = (String) properties.get("supportedImages");
      } catch (IOException ex) {
         log.warn("Could not load properties from " + CONFIGURATION_RESOURCE_NAME + ", using defaults", ex);
      }
      for (String image : images.split(",")) {
         supportedImages.add(image.trim());
      }
   }

   public static String convertToCommaSeparated(List<?> values) {
      StringBuffer buffer = new StringBuffer();
      for (Object obj : values) {
         if (obj != null) {
            buffer.append(obj.toString());
            buffer.append(",");
         }
      }
      return buffer.toString();
   }

   public static List<Integer> uploadAndStore(NodeManager manager, HttpServletRequest request) {
      List<UploadUtil.BinaryData> binaries = UploadUtil.uploadFiles(request, MAXSIZE);
      ArrayList<Integer> nodes = new ArrayList<Integer>();

      for (UploadUtil.BinaryData binary : binaries) {
         if (log.isDebugEnabled()) {
            log.debug("originalFileName: " + binary.getOriginalFileName());
            log.debug("contentType: " + binary.getContentType());
         }

         if (isZipFile(binary)) {
            log.debug("unzipping content");
            nodes.addAll(createNodesInZip(manager, new ZipInputStream(binary.getInputStream())));
         } else {
            Node node = createNode(manager, binary.getOriginalFileName(), binary.getInputStream(), binary.getLength());
            if (node != null) {
               nodes.add(node.getNumber());
            }
         }
      }
      return nodes;
   }

   public static List<Integer> store(Cloud cloud, NodeManager manager, String parentchannel, FormFile file) {
      ArrayList<Integer> nodes = new ArrayList<Integer>();
      if (StringUtils.isEmpty(parentchannel)) {
         throw new NullPointerException("parentchannel is null");
      }
      Node node = getNode(Integer.valueOf(parentchannel), manager, file);

      if (node != null) {
         nodes.add(node.getNumber());
      }
      return nodes;
   }

   private static boolean isZipFile(FormFile file) {

      for (String element : ZIP_MIME_TYPES) {
         if (element.equalsIgnoreCase(file.getContentType())) {
            return true;
         }
      }
      return false;
   }

   private static boolean isZipFile(BinaryData binary) {

      for (String element : ZIP_MIME_TYPES) {
         if (element.equalsIgnoreCase(binary.getContentType())) {
            return true;
         }
      }
      return false;
   }

   private static Node createNode(NodeManager manager, String fileName, InputStream in, long length) {
      if (length > manager.getField("handle").getMaxLength()) {
         return null;
      }
      Node node = manager.createNode();
      node.setValue("title", fileName);
      node.setValue("filename", fileName);
      node.setInputStreamValue("handle", in, length);
      node.commit();

      return node;
   }

   private static Node createNode(Integer parentChannel, NodeManager manager, String fileName, InputStream in,
         long length) {
      if (length > manager.getField("handle").getMaxLength()) {
         return null;
      }
      Node node = manager.createNode();
      node.setValue("title", fileName);
      node.setValue("filename", fileName);
      node.setInputStreamValue("handle", in, length);
      node.commit();

      RelationUtil.createRelation(node, manager.getCloud().getNode(parentChannel), "creationrel");

      return node;
   }

   private static Node getNode(Integer parentChannel, NodeManager manager, FormFile file) {
      Node node = null;
      try {
         if (isZipFile(file)) {
            ZipInputStream zip = new ZipInputStream(new BufferedInputStream(new ByteArrayInputStream(file.getFileData())));
            createNodesInZip(parentChannel, manager, zip);
         } else {
            createNode(parentChannel, manager, file.getFileName(), file.getInputStream(), file.getFileSize());
         }
      } catch (Exception ex) {
         log.error("Failed to read uploaded file", ex);
      } finally {
         file.destroy();
      }
      return node;
   }

   private static ArrayList<Integer> createNodesInZip(NodeManager manager, ZipInputStream zip) {

      ZipEntry entry = null;
      int count = 0;
      ArrayList<Integer> nodes = new ArrayList<Integer>();

      try {
         while ((entry = zip.getNextEntry()) != null) {
            if (entry.isDirectory()) {
               continue;
            }
            if ("images".equals(manager.getName()) && !isImage(entry.getName())) {
               if (log.isDebugEnabled()) {
                  log.debug("Skipping " + entry.getName() + " because it is not an image");
               }
               continue;
            }
            count++;
            // create temp file for zip entry, create a node from it and
            // remove the temp file
            File tempFile = File.createTempFile("cmsc", null);
            FileOutputStream out = new FileOutputStream(tempFile);
            copyStream(zip, out);
            zip.closeEntry();
            out.close();
            FileInputStream in = new FileInputStream(tempFile);
            Node node = createNode(manager, entry.getName(), in, tempFile.length());
            if (node != null) {
               nodes.add(node.getNumber());
            }
            in.close();
            tempFile.delete();
         }

      } catch (IOException ex) {
         log.info("Failed to read uploaded zipfile, skipping it"+ ex.getMessage());
      } finally {
         close(zip);
      }
      return nodes;
   }

   private static ArrayList<Integer> createNodesInZip(Integer parentChannel, NodeManager manager, ZipInputStream zip) {

      ZipEntry entry = null;
      int count = 0;
      ArrayList<Integer> nodes = new ArrayList<Integer>();

      try {
         while ((entry = zip.getNextEntry()) != null) {
            if (entry.isDirectory()) {
               continue;
            }
            if ("images".equals(manager.getName()) && !isImage(entry.getName())) {
               if (log.isDebugEnabled()) {
                  log.debug("Skipping " + entry.getName() + " because it is not an image");
               }
               continue;
            }
            count++;
            // create temp file for zip entry, create a node from it and
            // remove the temp file
            File tempFile = File.createTempFile("cmsc", null);
            FileOutputStream out = new FileOutputStream(tempFile);
            copyStream(zip, out);
            zip.closeEntry();
            out.close();
            FileInputStream in = new FileInputStream(tempFile);
            Node node = createNode(parentChannel, manager, entry.getName(), in, tempFile.length());
            if (node != null) {
               nodes.add(node.getNumber());
            }
            in.close();
            tempFile.delete();
         }

      } catch (IOException ex) {
         log.error("IOException--Failed to read uploaded zipfile, skipping it", ex);
      }catch(Exception e) {
         log.error("Failed to read uploaded zipfile, skipping it",e);
      }
      finally {
         close(zip);
      }
      return nodes;
   }

   private static void close(InputStream stream) {
      try {
         stream.close();
      } catch (IOException ignored) {
         // ignored
      }
   }

   private static boolean isImage(String fileName) {
      if (supportedImages == null) {
         initSupportedImages();
      }
      return fileName != null && supportedImages.contains(getExtension(fileName).toLowerCase());
   }

   private static String getExtension(String fileName) {
      int index = fileName.lastIndexOf('.');
      if (index < 0) {
         return null;
      }
      return fileName.substring(index);
   }

   private static void copyStream(InputStream ins, OutputStream outs) throws IOException {
      int bufferSize = 1024;
      byte[] writeBuffer = new byte[bufferSize];

      BufferedOutputStream bos = new BufferedOutputStream(outs, bufferSize);
      int bufferRead;
      while ((bufferRead = ins.read(writeBuffer)) != -1)
         bos.write(writeBuffer, 0, bufferRead);
      bos.flush();
      bos.close();
      outs.flush();
      outs.close();
   }

   public static void main(String[] args) {

      System.out.println(isImage(getExtension("test.jpg")));
      System.out.println(isImage(getExtension(".jpg")));
      System.out.println(isImage(getExtension("test")));
      System.out.println(isImage(getExtension("test.")));
      System.out.println(isImage(getExtension("")));
      System.out.println(isImage(getExtension("test.jpeg")));
      System.out.println(isImage(getExtension("test.gif")));
      System.out.println(isImage(getExtension("test.txt")));
      System.out.println(isImage(getExtension("test.bummer")));
      System.out.println(isImage(""));
      System.out.println(isImage(" "));
   }
}