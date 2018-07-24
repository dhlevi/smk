package ca.bc.gov.app.smks.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ektorp.Attachment;
import org.ektorp.AttachmentInputStream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import ca.bc.gov.app.smks.converter.DocumentConverterFactory;
import ca.bc.gov.app.smks.converter.DocumentConverterFactory.DocumentType;
import ca.bc.gov.app.smks.dao.CouchDAO;
import ca.bc.gov.app.smks.model.Attribute;
import ca.bc.gov.app.smks.model.Layer;
import ca.bc.gov.app.smks.model.MapConfigInfo;
import ca.bc.gov.app.smks.model.MapConfiguration;
import ca.bc.gov.app.smks.model.layer.Vector;

@CrossOrigin
@RestController
@RequestMapping("/MapConfigurations")
@PropertySource("classpath:application.properties")
public class MapConfigController
{
	private static Log logger = LogFactory.getLog(MapConfigController.class);

	@Autowired
	private CouchDAO couchDAO;

	@Autowired
    private Environment env;
	
	@RequestMapping(value = "/", method = RequestMethod.POST)
	@ResponseBody
	public ResponseEntity<?> createMapConfig(@RequestBody MapConfiguration request)
	{
		logger.debug(" >> createMapConfig()");
		ResponseEntity<?> result = null;

		try
		{
			logger.debug("    Creating new Map Configuration...");

			if(request.getName() == null) throw new Exception("The SMK ID is null. This is a required field");
			if(request.getName().length() == 0) throw new Exception("The SMK ID is empty. Please fill in a valid field");

			request.setLmfId(request.getName().toLowerCase().replaceAll(" ", "-").replaceAll("[^A-Za-z0-9]", "-"));
			
			request.setLmfRevision(1);
			request.setVersion(env.getProperty("smk.version"));

			// validate the ID, in case it's already in use.
			MapConfiguration existingDocID = couchDAO.getMapConfiguration(request.getLmfId());

			if(existingDocID != null)
			{
				// replace ID with a random guid
				request.setLmfId(UUID.randomUUID().toString());
			}

			couchDAO.createResource(request);
			logger.debug("    Success!");
			result = new ResponseEntity<String>("{ \"status\": \"Success\", \"couchId\": \"" + request.getId() + "\", \"lmfId\": \"" + request.getLmfId() + "\" }", HttpStatus.CREATED);
		}
		catch (Exception e)
		{
			logger.error("    ## Error creating Map Configuration resources " + e.getMessage());
			result = new ResponseEntity<String>("{ \"status\": \"ERROR\", \"message\": \"" + e.getMessage() + "\" }", HttpStatus.BAD_REQUEST);
		}

		logger.info("    Create New Map Configuration completed. Response: " + result.getStatusCode().name());
		logger.debug(" << createMapConfig()");
		return result;
	}

	@RequestMapping(value = "/", method = RequestMethod.GET)
	@ResponseBody
	public ResponseEntity<?> getAllMapConfigs()
	{
		logger.debug(" >> getAllMapConfigs()");
		ResponseEntity<?> result = null;

		try
		{
			logger.debug("    Querying for all Map Resources...");
			HashMap<String, String> resourceIds = couchDAO.getAllConfigs();
			logger.debug("    Success, found " + resourceIds.size() + " valid results");

			ArrayList<MapConfigInfo> configSnippets = new ArrayList<MapConfigInfo>();
			for(String configId : resourceIds.keySet())
			{
			    try
			    {
			        MapConfiguration config = couchDAO.getMapConfiguration(resourceIds.get(configId));
				    configSnippets.add(new MapConfigInfo(config));
			    }
			    catch(Exception e)
			    {
			        logger.debug("Map Configuration " + configId + " could not be loaded because it was invalid");
			        
			        // Add an empty config snippet
			        MapConfigInfo config = new MapConfigInfo();
			        config.setId(configId);
			        config.setName(resourceIds.get(configId));
			        config.setRevision(0);
			        config.setValid(false);
			        
			        configSnippets.add(config);
			    }
			}

			result = new ResponseEntity<ArrayList<MapConfigInfo>>(configSnippets, HttpStatus.OK);
		}
		catch (Exception e)
		{
			logger.error("    ## Error querying Map Config Resources: " + e.getMessage());
			result = new ResponseEntity<String>("{ \"status\": \"ERROR\", \"message\": \"" + e.getMessage() + "\" }", HttpStatus.BAD_REQUEST);
		}

		logger.info("    Get All Map Configurations completed. Response: " + result.getStatusCode().name());
		logger.debug(" << getAllMapConfigs()");
		return result;
	}

	@RequestMapping(value = "/{id}", method = RequestMethod.GET)
	@ResponseBody
	public ResponseEntity<?> getMapConfig(@PathVariable String id, @RequestParam(value="version", required=false) String version)
	{
		logger.debug(" >> getMapConfig()");
		ResponseEntity<?> result = null;

		try
		{
			logger.debug("    Fetching Map Configuration " + id);
			MapConfiguration resource = null;
			if(version != null && version.length() > 0)
			{
				logger.debug("    Getting version " + version);
				resource = couchDAO.getMapConfiguration(id, Integer.parseInt(version));
			}
			else
			{
				logger.debug("    Getting current version");
				resource = couchDAO.getMapConfiguration(id);
			}

			if(resource != null)
			{
				logger.debug("    Success!");
				result = new ResponseEntity<MapConfiguration>(resource, HttpStatus.OK);
			}
			else throw new Exception("Map Config not found for ID " + id + " and version " + version + " does not exist");
		}
		catch (Exception e)
		{
			logger.error("    ## Error querying Map Configuration resource " + id + ": " + e.getMessage());
			result = new ResponseEntity<String>("{ \"status\": \"ERROR\", \"message\": \"" + e.getMessage() + "\" }", HttpStatus.BAD_REQUEST);
		}

		logger.info("    Get Map Configuration completed. Response: " + result.getStatusCode().name());
		logger.debug(" << getMapConfig()");
		return result;
	}

	@RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
	@ResponseBody
	public ResponseEntity<?> deleteMapConfig(@PathVariable String id, @RequestParam(value="version", required=false) String version)
	{
		logger.debug(" >> deleteMapConfig()");
		ResponseEntity<?> result = null;

		try
		{
			logger.debug("    Deleting a Map Configuration...");

			// If we have a published version, cancel the delete and warn about un-publishing first
			MapConfiguration published = couchDAO.getPublishedConfig(id);
			if(published != null) throw new Exception("The Map Configuration has a published version. Please Un-Publish the configuration first via the {DELETE} /MapConfigurations/Published/{id} endpoint before deleting a configuration archive. ");
			else // nothing is published, so lets go forward with the delete process
			{
				// Should we be deleting just the requested config (may be many versions!) or all of the versions?

				// delete only the specified version
				if(version != null && version.length() > 0)
				{
					logger.debug("    Getting version " + version);
					MapConfiguration resource = couchDAO.getMapConfiguration(id, Integer.parseInt(version));

					if(resource != null)
					{
						logger.debug("    Deleting Map Configuration " + id + " version " + version);
						couchDAO.removeResource(resource);
						logger.debug("    Success!");
						result = new ResponseEntity<String>("{ \"status\": \"Success!\" }", HttpStatus.OK);
					}
					else throw new Exception("Map Config not found for ID " + id + " and version " + version + " does not exist");
				}
				// delete everything!
				else
				{
					logger.debug("    Deleting all versions");

					List<MapConfiguration> resources = couchDAO.getAllMapConfigurationVersions(id);
					for(MapConfiguration config : resources)
					{
						couchDAO.removeResource(config);
					}
					result = new ResponseEntity<String>("{ \"status\": \"Success!\" }", HttpStatus.OK);
				}
			}
		}
		catch (Exception e)
		{
			logger.error("    ## Error deleting map configuration: " + e.getMessage());
			result = new ResponseEntity<String>("{ \"status\": \"ERROR\", \"message\": \"" + e.getMessage() + "\" }", HttpStatus.BAD_REQUEST);
		}

		logger.info("    Delete Map Configuration completed. Response: " + result.getStatusCode().name());
		logger.debug(" << deleteMapConfig()");
		return result;
	}

	@RequestMapping(value = "/{id}", method = RequestMethod.PUT)
	@ResponseBody
	public ResponseEntity<?> updateMapConfig(@PathVariable String id, @RequestBody MapConfiguration request)
	{
		logger.debug(" >> updateMapConfig()");
		ResponseEntity<?> result = null;

		try
		{
			logger.debug("    Updating a Map Configuration...");

			if(request.isPublished()) throw new Exception("You cannot update the currently published Map Configuration. Please update the editable version.");
			
			MapConfiguration resource = couchDAO.getMapConfiguration(id);

		     // find out if we're removing layers. If so, we may have to remove attachments as well
			boolean layerRemoved = false;
            for(Layer originalLayer : resource.getLayers())
            {
                if(originalLayer.getType().equals("vector"))
                {
                    boolean exists = false;
                    for(Layer layer : request.getLayers())
                    {
                        if(layer.getType().equals("vector") && layer.getId().equals(originalLayer.getId()))
                        {
                            exists = true;
                        }
                    }
                    
                    if(!exists)
                    {
                        // remove attachment for this layer.
                        deleteAttachment(id, originalLayer.getId());
                        layerRemoved = true;
                    }
                }
            }
			
            // refresh, in case we've turfed any layer attachments
            if(layerRemoved) resource = couchDAO.getMapConfiguration(id);
            
            // Clone, to prevent removal of the attachments
			resource.setCreatedBy(request.getCreatedBy());
			resource.setId(request.getId());
			resource.setLayers(request.getLayers());
			resource.setLmfId(request.getLmfId());
			resource.setLmfRevision(request.getLmfRevision());
			resource.setName(request.getName());
			resource.setProject(request.getProject());
			resource.setPublished(request.isPublished());
			//resource.setRevision(request.getRevision());
			resource.setSurround(request.getSurround());
			resource.setTools(request.getTools());
			resource.setViewer(request.getViewer());
			resource.setVersion(request.getVersion());

			// Update!
			couchDAO.updateResource(resource);
			result = new ResponseEntity<String>("{ \"status\": \"Success!\" }", HttpStatus.OK);
		}
		catch (Exception e)
		{
			logger.error("    ## Error Updating Map Configuration: " + e.getMessage());
			result = new ResponseEntity<String>("{ \"status\": \"ERROR\", \"message\": \"" + e.getMessage() + "\" }", HttpStatus.BAD_REQUEST);
		}

		logger.info("    Update Map Configuration Completed. Response: " + result.getStatusCode().name());
		logger.debug(" << updateMapConfig()");
		return result;
	}

	@RequestMapping(value = "/{config_id}/Attachments", headers=("content-type=multipart/form-data"), method = RequestMethod.POST)
	@ResponseBody
	public ResponseEntity<?> createAttachment(@PathVariable String config_id, @RequestParam("file") MultipartFile request, @RequestParam("id") String id, @RequestParam("type") String type)
	{
		logger.debug(" >> createAttachment()");
		ResponseEntity<?> result = null;

		if(!request.isEmpty() && id != null && id.length() != 0)
		{
			try
			{
				logger.debug("    Creating new Attachment...");

				MapConfiguration resource = couchDAO.getMapConfiguration(config_id);

				if(resource != null)
				{
					// convert resource to geojson if it's a vector type
					byte[] docBytes = request.getBytes();
					
					String contentType = request.getContentType();
					
					if(contentType.equals("application/vnd.google-earth.kml+xml")) type = "kml";
					if(contentType.equals("application/vnd.google-earth.kmz")) type = "kmz";
					if(contentType.equals("application/zip")) type = "shape";
					if(contentType.equals("application/x-zip-compressed")) type = "shape";
					
					if(type.equals("kml")) { type = "vector"; docBytes = DocumentConverterFactory.convertDocument(request.getBytes(), DocumentType.KML); contentType = "application/octet-stream"; }
					if(type.equals("kmz")) { type = "vector"; docBytes = DocumentConverterFactory.convertDocument(request.getBytes(), DocumentType.KMZ); contentType = "application/octet-stream"; }
					else if(type.equals("csv")) { type = "vector"; docBytes = DocumentConverterFactory.convertDocument(request.getBytes(), DocumentType.CSV); contentType = "application/octet-stream"; }
					else if(type.equals("wkt")) { type = "vector"; docBytes = DocumentConverterFactory.convertDocument(request.getBytes(), DocumentType.WKT); contentType = "application/octet-stream"; }
					else if(type.equals("gml")) { type = "vector"; docBytes = DocumentConverterFactory.convertDocument(request.getBytes(), DocumentType.GML); contentType = "application/octet-stream"; }
					else if(type.equals("shape")) { type = "vector"; docBytes = DocumentConverterFactory.convertDocument(request.getBytes(), DocumentType.SHAPE); contentType = "application/octet-stream"; }
					
					Attachment attachment = new Attachment(id, Base64.encodeBase64String(docBytes), contentType);
				    resource.addInlineAttachment(attachment);

				    if(type.equals("image") && id.equals("surroundImage"))
				    {
				        resource.getSurround().setImageSrc("@surroundImage");
				    }

				    // if this is a geojson blob, make sure we have verified the properties set
				    if(type.equals("vector"))
				    {
				        Vector layer = (Vector)resource.getLayerByID(id);
				        
    				    ObjectMapper objectMapper = new ObjectMapper();
    				    JsonNode node = objectMapper.readValue(docBytes, JsonNode.class);

    				    List<String> fieldNames = new ArrayList<String>();
    				    
				        for (final JsonNode featureNode : node.get("features")) 
				        {
				            JsonNode properties = featureNode.get("properties");

				            for (Iterator<String> iter = properties.fieldNames(); iter.hasNext(); ) 
				            {
				                String fieldName = iter.next();
				                
				                if(!fieldName.equals("description") && !fieldNames.contains(fieldName))
				                {
				                    fieldNames.add(fieldName);
				                }
				            }
				        }
    				    
    				    //clear the attribute list
				        layer.getAttributes().clear();
				        
				        // add the new list
				        for(String field : fieldNames)
				        {
				            Attribute attr = new Attribute();
				            attr.setId(field);
				            attr.setName(field);
				            attr.setTitle(field.replace("-", " "));
				            attr.setVisible(true);
				            
				            layer.getAttributes().add(attr);
				        }
				    }

				    MapConfiguration updatedResource = couchDAO.getMapConfiguration(config_id);
				    resource.setRevision(updatedResource.getRevision());
				    couchDAO.updateResource(resource);

				    logger.debug("    Success!");
				    result = new ResponseEntity<String>("{ \"status\": \"Success!\" }", HttpStatus.OK);
				}
				else throw new Exception("Map Configuration ID not found.");
			}
			catch (Exception e)
			{
				logger.error("    ## Error creating attachment resource: " + e.getMessage());
				result = new ResponseEntity<String>("{ \"status\": \"ERROR\", \"message\": \"" + e.getMessage() + "\" }", HttpStatus.BAD_REQUEST);
			}
		}
		else result = new ResponseEntity<String>("{ \"status\": \"ERROR\", \"message\": \"File or ID was not submitted. Please post your form with a file, and id\" }", HttpStatus.BAD_REQUEST);

		logger.info("    Create New Attachment completed. Response: " + result.getStatusCode().name());
		logger.debug(" >> createAttachment()");
		return result;
	}

	@RequestMapping(value = "/{config_id}/Attachments", method = RequestMethod.GET)
	@ResponseBody
	public ResponseEntity<?> getAllAttachments(@PathVariable String config_id)
	{
		logger.debug(" >> getAllAttachments()");
		ResponseEntity<?> result = null;

		try
		{
			logger.debug("    Fetching all Attachments...");

			MapConfiguration resource = couchDAO.getMapConfiguration(config_id);

			if(resource != null)
			{
				ArrayList<String> attachments = new ArrayList<String>();

				for(String key : resource.getAttachments().keySet())
				{
					Attachment attachment = resource.getAttachments().get(key);
					attachments.add("{ \"id\": \"" + key + "\", \"content-type\": \"" + attachment.getContentType() + "\", \"content-length\": \"" + attachment.getContentLength() + "\"  }");
				}

				logger.debug("    Success!");
				result = new ResponseEntity<ArrayList<String>>(attachments, HttpStatus.OK);
			}
			else throw new Exception("Map Configuration ID not found.");
		}
		catch (Exception e)
		{
			logger.error("    ## Error fetching all attachment resource: " + e.getMessage());
			result = new ResponseEntity<String>("{ \"status\": \"ERROR\", \"message\": \"" + e.getMessage() + "\" }", HttpStatus.BAD_REQUEST);
		}

		logger.info("    Get All Attachments completed. Response: " + result.getStatusCode().name());
		logger.debug(" << getAllAttachments()");
		return result;
	}

	@RequestMapping(value = "/{config_id}/Attachments/{attachment_id}", method = RequestMethod.GET)
	@ResponseBody
	public ResponseEntity<?> getAttachment(@PathVariable String config_id, @PathVariable String attachment_id)
	{
		logger.debug(" >> getAttachment()");
		ResponseEntity<?> result = null;

		try
		{
			logger.debug("    Fetching an Attachment...");

			MapConfiguration resource = couchDAO.getMapConfiguration(config_id);

			if(resource != null)
			{
				AttachmentInputStream attachment = couchDAO.getAttachment(resource, attachment_id);
				byte[] media = IOUtils.toByteArray(attachment);

				final HttpHeaders httpHeaders= new HttpHeaders();
				MediaType contentType = MediaType.TEXT_PLAIN;

				if(attachment.getContentType() != null && attachment.getContentType().length() > 0)
				{
					String[] contentTypeVals = attachment.getContentType().split("/");
					contentType = new MediaType(contentTypeVals[0], contentTypeVals[1]);
				}

			    httpHeaders.setContentType(contentType);

				logger.debug("    Success!");
				result = new ResponseEntity<byte[]>(media, httpHeaders, HttpStatus.OK);
			}
			else throw new Exception("Map Configuration ID not found.");
		}
		catch (Exception e)
		{
			logger.error("    ## Error fetching attachment resource: " + e.getMessage());
			result = new ResponseEntity<String>("{ \"status\": \"ERROR\", \"message\": \"" + e.getMessage() + "\" }", HttpStatus.BAD_REQUEST);
		}

		logger.info("    Get Attachment completed. Response: " + result.getStatusCode().name());
		logger.debug(" << getAttachment()");
		return result;
	}

	@RequestMapping(value = "/{config_id}/Attachments/{attachment_id}", method = RequestMethod.DELETE)
	@ResponseBody
	public ResponseEntity<?> deleteAttachment(@PathVariable String config_id, @PathVariable String attachment_id)
	{
		logger.debug(" >> deleteAttachment()");
		ResponseEntity<?> result = null;

		try
		{
			logger.debug("    Deleting a Map Configuration Attachment...");
			MapConfiguration resource = couchDAO.getMapConfiguration(config_id);

			if(resource != null)
			{
				if(attachment_id != null && attachment_id.length() > 0 && resource.getAttachments().containsKey(attachment_id))
				{
					couchDAO.deleteAttachment(resource, attachment_id);
					logger.debug("    Success!");
					result = new ResponseEntity<String>("{ status: \"Success!\" }", HttpStatus.OK);
				}
				else throw new Exception("Attachment ID not found.");
			}
			else throw new Exception("Map Configuration ID not found.");
		}
		catch (Exception e)
		{
			logger.error("    ## Error deleting attachment: " + e.getMessage());
			result = new ResponseEntity<String>("{ \"status\": \"ERROR\", \"message\": \"" + e.getMessage() + "\" }", HttpStatus.BAD_REQUEST);
		}

		logger.info("    Delete Attachment completed. Response: " + result.getStatusCode().name());
		logger.debug(" << deleteAttachment()");
		return result;
	}

	@RequestMapping(value = "/{config_id}/Attachments/{attachment_id}", headers=("content-type=multipart/form-data"), method = RequestMethod.POST)
	@ResponseBody
	public ResponseEntity<?> updateAttachment(@PathVariable String config_id, @PathVariable String attachment_id, @RequestParam("file") MultipartFile request)
	{
		logger.debug(" >> updateAttachment()");
		ResponseEntity<?> result = null;

		if(!request.isEmpty())
		{
			try
			{
				logger.debug("    Updating Attachment " + attachment_id + "...");

				MapConfiguration resource = couchDAO.getMapConfiguration(config_id);

				if(resource != null)
				{
					couchDAO.deleteAttachment(resource, attachment_id);

					// fetch the updated resource, so we're not out of date
					resource = couchDAO.getMapConfiguration(config_id);

					Attachment attachment = new Attachment(attachment_id, Base64.encodeBase64String(request.getBytes()), request.getContentType());
					resource.addInlineAttachment(attachment);

					couchDAO.updateResource(resource);

				    logger.debug("    Success!");
				    result = new ResponseEntity<String>("{ status: \"Success!\" }", HttpStatus.OK);
				}
				else throw new Exception("Map Configuration ID not found.");
			}
			catch (Exception e)
			{
				logger.error("    ## Error fetching all attachment resource: " + e.getMessage());
				result = new ResponseEntity<String>("{ \"status\": \"ERROR\", \"message\": \"" + e.getMessage() + "\" }", HttpStatus.BAD_REQUEST);
			}
		}
		else result = new ResponseEntity<String>("{ \"status\": \"ERROR\", \"message\": \"File or ID was not submitted. Please post your form with a file, and id\" }", HttpStatus.BAD_REQUEST);

		logger.info("    Update Attachment completed. Response: " + result.getStatusCode().name());
		logger.debug(" << updateAttachment()");
		return result;
	}
}
