/*
 * Copyright 2014 Open mHealth
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.openmhealth.dsu.controller;

import com.github.fge.jackson.JsonLoader;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.google.common.collect.Range;

import utils.DataFile;
import utils.SchemaFile;
import utils.ValidationSummary;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.json.JSONException;
import org.json.simple.parser.JSONParser;
import org.json.JSONObject.*;
import org.json.JSONObject;
import org.json.*;
import org.openmhealth.dsu.domain.DataPointSearchCriteria;
import org.openmhealth.dsu.domain.EndUserUserDetails;
import org.openmhealth.dsu.service.DataPointService;
import org.openmhealth.schema.domain.omh.DataPoint;
import org.openmhealth.schema.domain.omh.DataPointHeader;
import org.openmhealth.schema.domain.omh.SchemaId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.examples.Utils;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import org.springframework.context.annotation.Bean;

import javax.validation.Valid;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.OffsetDateTime;
import java.util.*;
import java.security.MessageDigest;
import java.lang.Object.*;
import javax.crypto.spec.*;
import java.nio.charset.StandardCharsets;
import javax.crypto.Cipher;
import java.security.DigestException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import static org.openmhealth.dsu.configuration.OAuth2Properties.*;
import static org.springframework.http.HttpStatus.*;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.web.bind.annotation.RequestMethod.*; 


/**
 * A controller that handles the calls that read and write data points.
 *
 * @author Emerson Farrugia
 */
@ApiController
public class DataPointController {


    /*
     * These filtering parameters are temporary. They will likely change when a more generic filtering approach is
     * implemented.
     */
    public static final String CREATED_ON_OR_AFTER_PARAMETER = "created_on_or_after";
    public static final String CREATED_BEFORE_PARAMETER = "created_before";
    public static final String SCHEMA_NAMESPACE_PARAMETER = "schema_namespace";
    public static final String SCHEMA_NAME_PARAMETER = "schema_name";
    public static final String SCHEMA_VERSION_PARAMETER = "schema_version";
    public static final String USER_ID = "user_id";
    public static final String CAREGIVER_KEY = "CAREGIVER_KEY";
    public static final String RESULT_OFFSET_PARAMETER = "skip";
    public static final String RESULT_LIMIT_PARAMETER = "limit";
    public static final String DEFAULT_RESULT_LIMIT = "5000";
   

    @Autowired
    private DataPointService dataPointService;
    private static final Logger log = LoggerFactory.getLogger(DataPointController.class);

	// @Bean
	// public RestTemplate restTemplate(RestTemplateBuilder builder) {
	// 	return builder.build();
	// }
    /**
     * Reads data points.
     *
     * @param schemaNamespace the namespace of the schema the data points conform to
     * @param schemaName the name of the schema the data points conform to
     * @param schemaVersion the version of the schema the data points conform to
     * @param createdOnOrAfter the earliest creation timestamp of the data points to return, inclusive
     * @param createdBefore the latest creation timestamp of the data points to return, exclusive
     * @param offset the number of data points to skip
     * @param limit the number of data points to return
     * @return a list of matching data points
     */
    // TODO confirm if HEAD handling needs anything additional
    // only allow clients with read scope to read data points
    @PreAuthorize("#oauth2.clientHasRole('" + CLIENT_ROLE + "') and #oauth2.hasScope('" + DATA_POINT_READ_SCOPE + "')")
    // TODO look into any meaningful @PostAuthorize filtering
    @RequestMapping(value = "/dataPoints", method = {HEAD, GET}, produces = APPLICATION_JSON_VALUE)
    public
    @ResponseBody
    ResponseEntity<Iterable<DataPoint>> readDataPoints(
            @RequestParam(value = SCHEMA_NAMESPACE_PARAMETER) final String schemaNamespace,
            @RequestParam(value = SCHEMA_NAME_PARAMETER) final String schemaName,
            // TODO make this optional and update all associated code
            @RequestParam(value = SCHEMA_VERSION_PARAMETER) final String schemaVersion,
            // TODO replace with Optional<> in Spring MVC 4.1
            @RequestParam(value = CREATED_ON_OR_AFTER_PARAMETER, required = false)
            final OffsetDateTime createdOnOrAfter,
            @RequestParam(value = CREATED_BEFORE_PARAMETER, required = false) final OffsetDateTime createdBefore,
            @RequestParam(value = RESULT_OFFSET_PARAMETER, defaultValue = "0") final Integer offset,
            @RequestParam(value = RESULT_LIMIT_PARAMETER, defaultValue = DEFAULT_RESULT_LIMIT) final Integer limit,
            Authentication authentication
            ) {

        // TODO add validation or explicitly comment that this is handled using exception translators

        // determine the user associated with the access token to restrict the search accordingly
        String endUserId = getEndUserId(authentication);

        DataPointSearchCriteria searchCriteria =
                new DataPointSearchCriteria(endUserId, schemaNamespace, schemaName, schemaVersion);

        if (createdOnOrAfter != null && createdBefore != null) {
            searchCriteria.setCreationTimestampRange(Range.closedOpen(createdOnOrAfter, createdBefore));
        }
        else if (createdOnOrAfter != null) {
            searchCriteria.setCreationTimestampRange(Range.atLeast(createdOnOrAfter));
        }
        else if (createdBefore != null) {
            searchCriteria.setCreationTimestampRange(Range.lessThan(createdBefore));
        }

        Iterable<DataPoint> dataPoints = dataPointService.findBySearchCriteria(searchCriteria, offset, limit);

        HttpHeaders headers = new HttpHeaders();

        // FIXME add pagination headers
        // headers.set("Next");
        // headers.set("Previous");

        return new ResponseEntity<>(dataPoints, headers, OK);
    }
    

    
    @PreAuthorize("#oauth2.clientHasRole('" + CLIENT_ROLE + "') and #oauth2.hasScope('" + DATA_POINT_READ_SCOPE + "')")
    // TODO look into any meaningful @PostAuthorize filtering
    @RequestMapping(value = "/dataPoints/caregiver", method = {HEAD, GET}, produces = APPLICATION_JSON_VALUE)
    public
    @ResponseBody
    ResponseEntity<Iterable<DataPoint>> readDataPointsCareGiver(
            @RequestParam(value = SCHEMA_NAMESPACE_PARAMETER) final String schemaNamespace,
            @RequestParam(value = SCHEMA_NAME_PARAMETER) final String schemaName,
            // TODO make this optional and update all associated code
            @RequestParam(value = SCHEMA_VERSION_PARAMETER) final String schemaVersion,
            @RequestParam(value = USER_ID) final String endUserId,
            @RequestParam(value = CAREGIVER_KEY) final String careGiverKey,
            // TODO replace with Optional<> in Spring MVC 4.1
            @RequestParam(value = CREATED_ON_OR_AFTER_PARAMETER, required = false)
            final OffsetDateTime createdOnOrAfter,
            @RequestParam(value = CREATED_BEFORE_PARAMETER, required = false) final OffsetDateTime createdBefore,
            @RequestParam(value = RESULT_OFFSET_PARAMETER, defaultValue = "0") final Integer offset,
            @RequestParam(value = RESULT_LIMIT_PARAMETER, defaultValue = DEFAULT_RESULT_LIMIT) final Integer limit,
            Authentication authentication) {

    	if (!careGiverKey.equals("someKey"))
    	{
    		return new ResponseEntity<>(NOT_ACCEPTABLE);
    	}
        DataPointSearchCriteria searchCriteria =
                new DataPointSearchCriteria(endUserId, schemaNamespace, schemaName, schemaVersion);

        if (createdOnOrAfter != null && createdBefore != null) {
            searchCriteria.setCreationTimestampRange(Range.closedOpen(createdOnOrAfter, createdBefore));
        }
        else if (createdOnOrAfter != null) {
            searchCriteria.setCreationTimestampRange(Range.atLeast(createdOnOrAfter));
        }
        else if (createdBefore != null) {
            searchCriteria.setCreationTimestampRange(Range.lessThan(createdBefore));
        }

        Iterable<DataPoint> dataPoints = dataPointService.findBySearchCriteria(searchCriteria, offset, limit);

        HttpHeaders headers = new HttpHeaders();

        // FIXME add pagination headers
        // headers.set("Next");
        // headers.set("Previous");

        return new ResponseEntity<>(dataPoints, headers, OK);
    }

    public String getEndUserId(Authentication authentication) {

        return ((EndUserUserDetails) authentication.getPrincipal()).getUsername();
    }

    /**
     * Reads a data point.
     *
     * @param id the identifier of the data point to read
     * @return a matching data point, if found
     */
    // TODO can identifiers be relative, e.g. to a namespace?
    // TODO confirm if HEAD handling needs anything additional
    // only allow clients with read scope to read a data point
    @PreAuthorize("#oauth2.clientHasRole('" + CLIENT_ROLE + "') and #oauth2.hasScope('" + DATA_POINT_READ_SCOPE + "')")
    // ensure that the returned data point belongs to the user associated with the access token
    @PostAuthorize("returnObject.body == null || returnObject.body.header.userId == principal.username")
    @RequestMapping(value = "/dataPoints/{id}", method = {HEAD, GET}, produces = APPLICATION_JSON_VALUE)
    public
    @ResponseBody
    ResponseEntity<DataPoint> readDataPoint(@PathVariable String id) {

        Optional<DataPoint> dataPoint = dataPointService.findOne(id);

        if (!dataPoint.isPresent()) {
            return new ResponseEntity<>(NOT_FOUND);
        }

        // FIXME test @PostAuthorize
        return new ResponseEntity<>(dataPoint.get(), OK);
    }

    /**
     * Starts a Sieve Import
     */
    @PreAuthorize("#oauth2.clientHasRole('" + CLIENT_ROLE + "') and #oauth2.hasScope('" + DATA_POINT_READ_SCOPE + "')")
    @RequestMapping(value = "/sieve", method = {HEAD,GET}, produces = APPLICATION_JSON_VALUE)
    public @ResponseBody ResponseEntity<?> startSieveImport() {
        // String url = "https://localhost:5050/storage/import";
        // return this.restTemplate.getForObject(url, String.class);

  // String command = "curl -X GET http://backend:5000/storage/import";
        try {
            URL url = new URL("storage_provider:5000/storage/import");

            HttpURLConnection http = (HttpURLConnection)url.openConnection();
            http.setRequestProperty("Accept", "application/json");
    
            System.out.println(http.getResponseCode() + " " + http.getResponseMessage());

            http.disconnect();
        } catch (IOException e)
        {
            e.printStackTrace();
        }

        
        // try {
        //     Process process = Runtime.getRuntime().exec(command);
        //     InputStream input = process.getInputStream();
        //     int code = process.exitValue();
        //     process.destroy();
        // } catch (IOException e) {
        //     e.printStackTrace();
        // }
        return new ResponseEntity<>(0 == 0 ? NOT_FOUND : OK);

    }

    /**
     * Receives ACS and ABE encryption keys from Sieve client
     */
    @PreAuthorize("#oauth2.clientHasRole('" + CLIENT_ROLE + "') and #oauth2.hasScope('" + DATA_POINT_READ_SCOPE + "')")
    @RequestMapping(value = "/sieve/{id}", method = GET, produces = APPLICATION_JSON_VALUE)
    public @ResponseBody ResponseEntity<?> receiveKeys(@PathVariable String id )
    {
        // String[] values = acs.split(",");
        String cipherText = "";
        String decryptedText = ""; 
        BufferedReader rd  = null;
        StringBuilder sb = null;
        String line = null;
        String k = "asdjk@15r32r1234asdsaeqwe314SEFT";
        try {
           // id = data.getString("GUID");
           // k = data.getString("k");
            URL url = new URL("http://storage_provider:5000/storage/"+id);
            HttpURLConnection http = (HttpURLConnection)url.openConnection();
            http.setRequestMethod("GET");
            http.connect();
            int responseCode = http.getResponseCode();
            System.out.println("GET Response Code :: " + responseCode);
            rd  = new BufferedReader(new InputStreamReader(http.getInputStream()));
            sb = new StringBuilder();
  
            while ((line = rd.readLine()) != null)
            {
                System.out.println(line);
                sb.append(line);
            }
            System.out.println("sb: "+ sb);
            JSONObject json = new JSONObject(new JSONTokener(sb.toString()));
            System.out.println("sb: "+ sb.toString());
            String result = json.getJSONObject("result").getString("value");
            // System.out.println("result: "+ result);
            // JSONObject val = new JSONObject(new JSONTokener(result.toString()));
            cipherText = result;
            System.out.println("Ciphertext: "+ cipherText);
            //return sb.toString();
            http.disconnect();
        } catch (Exception e)
        {
            e.printStackTrace();
            return new ResponseEntity<>(decryptedText, NOT_FOUND);
        }

        // String command = "curl -X GET http://backend:5000/storage/ABE/"+id + "-H 'accept: application/json'";
        
        // try {
        //     Process process = Runtime.getRuntime().exec(command);
        //     InputStream input = process.getInputStream();
        //     int code = process.exitValue();
        //     process.destroy();
        // } catch (IOException e) {
        //     e.printStackTrace();
        // }
        try {
            byte[] cipherData = Base64.getDecoder().decode(cipherText);
            byte[] saltData = Arrays.copyOfRange(cipherData, 8, 16);
            String secret = k;
    
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            final byte[][] keyAndIV = GenerateKeyAndIV(32, 16, 1, saltData, secret.getBytes(StandardCharsets.UTF_8), md5);
            SecretKeySpec key = new SecretKeySpec(keyAndIV[0], "AES");
            IvParameterSpec iv = new IvParameterSpec(keyAndIV[1]);
    
            byte[] encrypted = Arrays.copyOfRange(cipherData, 16, cipherData.length);
            Cipher aesCBC = Cipher.getInstance("AES/CBC/PKCS5Padding");
            aesCBC.init(Cipher.DECRYPT_MODE, key, iv);
            byte[] decryptedData = aesCBC.doFinal(encrypted);
            decryptedText = new String(decryptedData, StandardCharsets.UTF_8);
            
            System.out.println(decryptedText);
        } catch (Exception e)
        {
            e.printStackTrace();
            return new ResponseEntity<>(decryptedText, NOT_FOUND);
        }

        return new ResponseEntity<>(decryptedText, OK);

    }

    /**
 * Generates a key and an initialization vector (IV) with the given salt and password.
 * <p>
 * This method is equivalent to OpenSSL's EVP_BytesToKey function
 * (see https://github.com/openssl/openssl/blob/master/crypto/evp/evp_key.c).
 * By default, OpenSSL uses a single iteration, MD5 as the algorithm and UTF-8 encoded password data.
 * </p>
 * @param keyLength the length of the generated key (in bytes)
 * @param ivLength the length of the generated IV (in bytes)
 * @param iterations the number of digestion rounds 
 * @param salt the salt data (8 bytes of data or <code>null</code>)
 * @param password the password data (optional)
 * @param md the message digest algorithm to use
 * @return an two-element array with the generated key and IV
 */
public static byte[][] GenerateKeyAndIV(int keyLength, int ivLength, int iterations, byte[] salt, byte[] password, MessageDigest md) {

    int digestLength = md.getDigestLength();
    int requiredLength = (keyLength + ivLength + digestLength - 1) / digestLength * digestLength;
    byte[] generatedData = new byte[requiredLength];
    int generatedLength = 0;

    try {
        md.reset();

        // Repeat process until sufficient data has been generated
        while (generatedLength < keyLength + ivLength) {

            // Digest data (last digest if available, password data, salt if available)
            if (generatedLength > 0)
                md.update(generatedData, generatedLength - digestLength, digestLength);
            md.update(password);
            if (salt != null)
                md.update(salt, 0, 8);
            md.digest(generatedData, generatedLength, digestLength);

            // additional rounds
            for (int i = 1; i < iterations; i++) {
                md.update(generatedData, generatedLength, digestLength);
                md.digest(generatedData, generatedLength, digestLength);
            }

            generatedLength += digestLength;
        }

        // Copy key and IV into separate byte arrays
        byte[][] result = new byte[2][];
        result[0] = Arrays.copyOfRange(generatedData, 0, keyLength);
        if (ivLength > 0)
            result[1] = Arrays.copyOfRange(generatedData, keyLength, keyLength + ivLength);

        return result;

    } catch (DigestException e) {
        throw new RuntimeException(e);

    } finally {
        // Clean out temporary data
        Arrays.fill(generatedData, (byte)0);
    }
}

    /**
     * Unencrypts and then writes a data point.
     *
     * @param dataPoint the data point to write
     * @throws URISyntaxException 
     * @throws ProcessingException 
     * @throws IOException 
     * @throws JsonProcessingException 
     */
    // only allow clients with write scope to write data points
    @PreAuthorize("#oauth2.clientHasRole('" + CLIENT_ROLE + "') and #oauth2.hasScope('" + DATA_POINT_WRITE_SCOPE + "')")
    @RequestMapping(value = "/dataPoints", method = POST, consumes = APPLICATION_JSON_VALUE)
    public ResponseEntity<?> writeDataPoint(@RequestBody @Valid DataPoint dataPoint, Authentication authentication) throws URISyntaxException, ProcessingException, JsonProcessingException, IOException {
    	
    	DataPointValidatorUnit validator = new DataPointValidatorUnit(dataPoint);
    	if (! validator.isValidBody()) {
    		return new ResponseEntity<>(NOT_ACCEPTABLE);
    	}
 	   
 	   
       if (dataPointService.exists(dataPoint.getHeader().getId())) {
        	
            return new ResponseEntity<>(CONFLICT);
       } 

       String endUserId = getEndUserId(authentication);

       // set the owner of the data point to be the user associated with the access token
       setUserId(dataPoint.getHeader(), endUserId);

       dataPointService.save(dataPoint);

       return new ResponseEntity<>(CREATED);
    }

    // this is currently implemented using reflection, until we see other use cases where mutability would be useful
    private void setUserId(DataPointHeader header, String endUserId) {
        try {
            Field userIdField = header.getClass().getDeclaredField("userId");
            userIdField.setAccessible(true);
            userIdField.set(header, endUserId);
        }
        catch (NoSuchFieldException | IllegalAccessException e) {
            throw new IllegalStateException("A user identifier property can't be changed in the data point header.", e);
        }
    }
    

    /**
     * Deletes a data point.
     *
     * @param id the identifier of the data point to delete
     */
    // only allow clients with delete scope to delete data points
    @PreAuthorize(
            "#oauth2.clientHasRole('" + CLIENT_ROLE + "') and #oauth2.hasScope('" + DATA_POINT_DELETE_SCOPE + "')")
    @RequestMapping(value = "/dataPoints/{id}", method = RequestMethod.DELETE)
    public ResponseEntity<?> deleteDataPoint(@PathVariable String id, Authentication authentication) {

        String endUserId = getEndUserId(authentication);

        // only delete the data point if it belongs to the user associated with the access token
        Long dataPointsDeleted = dataPointService.deleteByIdAndUserId(id, endUserId);

        return new ResponseEntity<>(dataPointsDeleted == 0 ? NOT_FOUND : OK);
    }
}
