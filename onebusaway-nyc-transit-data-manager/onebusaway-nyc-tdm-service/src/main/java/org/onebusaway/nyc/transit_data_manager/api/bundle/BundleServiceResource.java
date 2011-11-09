package org.onebusaway.nyc.transit_data_manager.api.bundle;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import org.onebusaway.nyc.transit_data_manager.api.CrewResource;
import org.onebusaway.nyc.transit_data_manager.bundle.BundleProvider;
import org.onebusaway.nyc.transit_data_manager.bundle.BundlesListMessage;
import org.onebusaway.nyc.transit_data_manager.bundle.model.Bundle;
import org.onebusaway.nyc.transit_data_manager.json.JsonTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sun.jersey.core.header.ContentDisposition;

@Path("/bundle")
@Component
public class BundleServiceResource {

  private static Logger _log = LoggerFactory.getLogger(CrewResource.class);
  
  @Autowired
  BundleProvider bundleProvider;
  @Autowired
  JsonTool jsonTool;

  public void setBundleProvider(BundleProvider bundleProvider) {
    this.bundleProvider = bundleProvider;
  }

  public void setJsonTool(JsonTool jsonTool) {
    this.jsonTool = jsonTool;
  }

  @Path("/list")
  @GET
  public Response getBundleList() {
    List<Bundle> bundles = bundleProvider.getBundles();

    Response response;

    BundlesListMessage bundlesMessage = new BundlesListMessage();

    if (bundles != null) {
      bundlesMessage.setBundles(bundles);
      bundlesMessage.setStatus("OK");

      final BundlesListMessage bundlesMessageToWrite = bundlesMessage;

      StreamingOutput output = new StreamingOutput() {

        @Override
        public void write(OutputStream out) throws IOException,
            WebApplicationException {
          BufferedWriter writer = new BufferedWriter(
              new OutputStreamWriter(out));

          jsonTool.writeJson(writer, bundlesMessageToWrite);

          writer.close();
          out.close();

        }
      };
      response = Response.ok(output, "application/json").build();
    } else {
      response = Response.serverError().build();
    }

    return response;
  }

  @Path("/{bundleId}/file/{bundleFileFilename}/get")
  @GET
  public Response getBundleFile(@PathParam("bundleId") String bundleId,
      @PathParam("bundleFileFilename") String relativeFilename) {

    _log.info("starting BundleServiceResource.getBundleFile");
    Response response;
    
    File requestedFile = bundleProvider.getBundleFile(bundleId, relativeFilename);
    
    
    
    if (requestedFile != null) {
      
      _log.info("requested " + requestedFile.getPath());
      
      final File fileToSend = requestedFile;
      
      StreamingOutput output = new StreamingOutput() {
        
        @Override
        public void write(OutputStream os) throws IOException,
            WebApplicationException {

          InputStream is = new BufferedInputStream(new FileInputStream(fileToSend));
          
          int next;
          while ((next = is.read()) != -1) {
            os.write(next);
          }
          
          is.close();
          os.close();
        }
      };
      
      ContentDisposition cd = ContentDisposition.type("file").fileName(fileToSend.getName()).build();
      response = Response.ok(output, MediaType.APPLICATION_OCTET_STREAM).header("Content-Disposition", cd).build();
    } else {
      _log.info("requestedFile was null");
      response = null;
      throw new WebApplicationException(new Exception("In BundleServiceResource.getBundleFile, requestedFile was null."));
    }
    
    return response;
  }
}