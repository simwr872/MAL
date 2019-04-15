package com.foreseeti.generator;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.util.Base64;

import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.PNGTranscoder;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;

import se.kth.mal.Asset;
import se.kth.mal.CompilerModel;

public class SecuriCADCodeGeneratorUsingTemplates extends SecuriCADCodeGenerator {
   public SecuriCADCodeGeneratorUsingTemplates(String malFilePath, String testCasesFolder, String javaFolder, String packageName, String visualFolderPath) throws IllegalArgumentException {
      super(malFilePath, testCasesFolder, javaFolder, packageName, visualFolderPath);
   }

   private String getVisual(String assetName) {
      if (this.visualFolder == null)
         return "";
      File[] files = this.visualFolder.listFiles();
      for (File file : files) {
         String[] fileComponents = file.getName().split("\\.");
         String fileName = fileComponents[0];
         String fileExtension = fileComponents[1].toLowerCase();
         if (!fileName.equals(assetName)) {
            continue;
         }
         try {
            byte[] fileBytes = Files.readAllBytes(file.toPath());
            if (fileExtension.equals("svg")) {
               // Browser supports base64 encoding of xml but securicad does not
               TranscoderInput svgInput = new TranscoderInput(file.toPath().toString());
               ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
               TranscoderOutput pngOutput = new TranscoderOutput(byteOut);
               new PNGTranscoder().transcode(svgInput, pngOutput);
               fileBytes = byteOut.toByteArray();
               fileExtension = "png";
            }
            byte[] encodedBytes = Base64.getEncoder().encode(fileBytes);
            return String.format("data:image/%s;base64,%s", fileExtension, new String(encodedBytes));
         }
         catch (IOException | TranscoderException e) {
            e.printStackTrace();
         }
      }
      return "";
   }

   @Override
   protected void writeJava(String outputFolder, String packageName, String packagePath) throws IOException, UnsupportedEncodingException {
      // Create the path unless it already exists
      String path = outputFolder + "/" + packagePath + "/";
      (new File(path)).mkdirs();
      for (Asset asset : model.getAssets()) {
         System.out.println("Writing the Java class corresponding to asset " + asset.getName());
         String sourceCodeFile = path + asset.getName() + ".java";
         writer = new PrintWriter(sourceCodeFile, "UTF-8");
         VelocityEngine ve = new VelocityEngine();
         ve.setProperty(RuntimeConstants.RESOURCE_LOADER, "classpath");
         ve.setProperty("classpath.resource.loader.class", ClasspathResourceLoader.class.getName());
         ve.setProperty("file.resource.loader.path", this.getClass().getResource("/").getPath());
         ve.init();
         Template template = ve.getTemplate("Asset.vm");
         VelocityContext context = new VelocityContext();
         context.put("model", model);
         context.put("packageName", packageName);
         context.put("importList", getImportList());
         context.put("asset", asset);
         context.put("CompilerModel", CompilerModel.class);
         context.put("icon", getVisual(asset.getName()));
         template.merge(context, writer);
         writer.close();

      }
      System.out.println("Writing AutoLangLink ");
      String sourceCodeFile = path + "AutoLangLink.java";
      writer = new PrintWriter(sourceCodeFile, "UTF-8");
      VelocityEngine ve = new VelocityEngine();
      ve.setProperty(RuntimeConstants.RESOURCE_LOADER, "classpath");
      ve.setProperty("classpath.resource.loader.class", ClasspathResourceLoader.class.getName());
      ve.setProperty("file.resource.loader.path", this.getClass().getResource("/").getPath());
      ve.init();
      Template template = ve.getTemplate("AutoLangLink.vm");
      VelocityContext context = new VelocityContext();
      context.put("model", model);
      context.put("packageName", packageName);
      context.put("CompilerModel", CompilerModel.class);
      template.merge(context, writer);
      writer.close();

      System.out.println("Writing Attacker ");
      sourceCodeFile = path + "Attacker.java";
      writer = new PrintWriter(sourceCodeFile, "UTF-8");
      ve = new VelocityEngine();
      ve.setProperty(RuntimeConstants.RESOURCE_LOADER, "classpath");
      ve.setProperty("classpath.resource.loader.class", ClasspathResourceLoader.class.getName());
      ve.setProperty("file.resource.loader.path", this.getClass().getResource("/").getPath());
      ve.init();
      template = ve.getTemplate("Attacker.vm");
      context = new VelocityContext();
      context.put("model", model);
      context.put("importList", getImportList());
      context.put("packageName", packageName);
      context.put("CompilerModel", CompilerModel.class);
      context.put("icon", getVisual("Attacker"));
      template.merge(context, writer);
      writer.close();

   }
}
