package com.example.stix;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.jena.base.Sys;
import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.vocabulary.XSD;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.http.HTTPRepository;
import org.eclipse.rdf4j.rio.RDFFormat;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.regex.Pattern;

public class StixParser {

    private static final String UCO_NAMESPACE = "https://unifiedcyberontology.org/ontology#";
    private static final Map<String, Resource> resources = new HashMap<>();
    private static Resource Stixthing = null;

    public static void main(String[] args) {
/*

     String stixFilePath = "/stix.json"; // Path to the STIX JSON file

        try (InputStream inputStream = StixParser.class.getResourceAsStream(stixFilePath);
             InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {

*/

        String datasetPath = "C:\\Users\\user\\Downloads\\Samples"; // Update this to the path where the STIX files are located
          //String datasetPath="C:\\Users\\user\\Downloads\\example";
        File folder = new File(datasetPath);
        File[] listOfFiles = folder.listFiles((dir, name) -> name.endsWith(".json"));

        if (listOfFiles != null) {

            for (File file : listOfFiles) {
                if (file.isFile()) {
                    System.out.println("Processing file: " + file.getName());

                    try (InputStream inputStream = new FileInputStream(file);


                         InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {

                        JsonElement rootElement = JsonParser.parseReader(reader);
                        JsonObject stixJson = rootElement.getAsJsonObject();
                        JsonArray objects = stixJson.getAsJsonArray("objects");

                        Model model = ModelFactory.createDefaultModel();
                        model.setNsPrefix("uco", UCO_NAMESPACE);
                        Stixthing = model.createResource(UCO_NAMESPACE + stixJson.get("id").getAsString()).addProperty(RDF.type, RDFS.Class);
                        // Define common STIX types as RDF classes

                        // Add other STIX types as needed

                        for (JsonElement jsonElement : objects) {
                            JsonObject stixObject = jsonElement.getAsJsonObject();
                            handleStixObject(stixObject, model);
                        }

                        model.write(System.out, "TURTLE");

                        String graphDbUrl = "http://localhost:7200"; // GraphDB URL
                        String repositoryId = "UCO"; // Update with your  repository ID
                        String username = "admin"; // GraphDB username
                        String password = "stix123"; // GraphDB password
                        storeRdfInGraphDb(model, graphDbUrl, repositoryId, username, password);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

        public static void handleStixObject (JsonObject stixObject, Model model) {
            String id = getAsString(stixObject, "id");
            String type = getAsString(stixObject, "type");

            if (id == null || type == null) {
                return; //
            }
            Resource resource = model.createResource(UCO_NAMESPACE + id);
            if (type.equals("relationship")) {Resource relationshipClass = model.createResource(UCO_NAMESPACE + "Relationship").addProperty(RDF.type, RDFS.Class);
                String relationshipType = mapStixToUcoRelationship(getAsString(stixObject, "relationship_type"));
                Resource PropertyClass = model.createResource(UCO_NAMESPACE + relationshipType).addProperty(RDF.type, OWL.ObjectProperty);
                // resource.addProperty(RDF.type,RDF.Property);
                model.add(Stixthing, RDFS.subClassOf, relationshipClass);
                model.add(resource, RDF.type, relationshipClass);
            } else if (type.equals("sighting")) {
                // Resource sightingClass = model.createResource(UCO_NAMESPACE + "Sighting").addProperty(RDFS.subClassOf, Stixthing);
                Resource sightingClass = model.createResource(UCO_NAMESPACE + "Sighing").addProperty(RDF.type, RDFS.Class);
                model.add(Stixthing, RDFS.subClassOf, sightingClass);
                model.add(resource, RDF.type, sightingClass);
            }
            else if(type.equals("course-of-action"))
            {
                Resource ucoClass = model.createResource(UCO_NAMESPACE +"CourseOfAction").addProperty(RDF.type, RDFS.Class);
                model.add(Stixthing,RDFS.subClassOf,ucoClass);
                model.add(resource, RDF.type, ucoClass);
        }else //if ("malware".equals(type)) {
            {
               // Resource ucoClass = model.createResource(UCO_NAMESPACE + ucoclass(type)).addProperty(RDFS.subClassOf, Stixthing);
                 Resource ucoClass = model.createResource(UCO_NAMESPACE +ucoclass(type)).addProperty(RDF.type, RDFS.Class);
                model.add(Stixthing,RDFS.subClassOf,ucoClass);
                 model.add(resource, RDF.type, ucoClass);
            }
            resources.put(id, resource);

            for (Map.Entry<String, JsonElement> entry : stixObject.entrySet()) {
                String key = entry.getKey();
                if (!key.equals("id") && !key.equals("type")) {
                    JsonElement value = entry.getValue();
                    handleProperty(resource, key, value, model);
                }
            }
            if (stixObject.has("external_references")) {
                handleExternalReferences(stixObject.getAsJsonArray("external_references"), resource, model);
            }
            handleReferences(stixObject, resource, model);
        }

        private static void handleProperty (Resource resource, String key, JsonElement value, Model model){
            String ucoProperty = mapStixToUcoProperty(key);
            Literal subjectLiteral;
            Property property = ResourceFactory.createProperty(UCO_NAMESPACE, ucoProperty);
            if (key.equals("kill_chain_phases") && value.isJsonArray()) {
                model.createResource(UCO_NAMESPACE+"KillChainPhase").addProperty(RDF.type,RDFS.Class);
                JsonArray killChainArray = value.getAsJsonArray();
                for (JsonElement killChainElement : killChainArray) {
                    if (killChainElement.isJsonObject()) {
                        JsonObject killChainObject = killChainElement.getAsJsonObject();
                        String killChainName = getAsString(killChainObject, "kill_chain_name");
                        String phaseName = getAsString(killChainObject, "phase_name");


                        String killChainURI = UCO_NAMESPACE + "kill_chain" + "--"+killChainName + "" ;
                        Resource killChainPhaseResource = model.createResource(killChainURI)
                                .addProperty(RDF.type, ResourceFactory.createResource(UCO_NAMESPACE + "KillChainPhase"));

                        Property killChainNameProperty = ResourceFactory.createProperty(UCO_NAMESPACE, "killChainName");
                        Property phaseNameProperty = ResourceFactory.createProperty(UCO_NAMESPACE, "phaseName");


                        killChainPhaseResource.addLiteral(killChainNameProperty, killChainName);
                        killChainPhaseResource.addLiteral(phaseNameProperty, phaseName);
                        model.add(killChainNameProperty, RDF.type, OWL.DatatypeProperty);
                        model.add(phaseNameProperty, RDF.type, OWL.DatatypeProperty);

                        resource.addProperty(property, killChainPhaseResource);


                        model.add(property, RDF.type, OWL.ObjectProperty);
                    }
                }
            }
            else if (key.endsWith("_refs") && value.isJsonArray()) {

                JsonArray refsArray = value.getAsJsonArray();
                for (JsonElement refElement : refsArray) {
                    if (refElement.isJsonPrimitive()) {
                        String referencedId = refElement.getAsString();
                        Resource referencedResource = model.createResource(UCO_NAMESPACE + referencedId);
                        resource.addProperty(property, referencedResource);
                        model.add(property, RDF.type, OWL.ObjectProperty); // Declare as an Object Property
                    }
                }
            } else if (value.isJsonPrimitive()) {

                if (detectLiteralType(value.getAsString()) == "Integer") {
                    subjectLiteral = model.createTypedLiteral(value.getAsString(), XSD.integer.getURI());

                } else if (detectLiteralType(value.getAsString()) == "Date") {
                    subjectLiteral = model.createTypedLiteral(value.getAsString(), XSD.date.getURI());
                } else if (detectLiteralType(value.getAsString()) == "Float") {
                    subjectLiteral = model.createTypedLiteral(value.getAsString(), XSD.xfloat.getURI());
                } else if (detectLiteralType(value.getAsString()) == "Boolean") {
                    subjectLiteral = model.createTypedLiteral(value.getAsString(), XSD.xboolean.getURI());
                } else {
                    subjectLiteral = model.createTypedLiteral(value.getAsString(), XSD.xstring.getURI());
                }
                model.add(property, RDF.type, OWL.DatatypeProperty);
                resource.addProperty(property, subjectLiteral);
                model.add(resource, property, subjectLiteral);


                // model.add(resource,property, subjectLiteral);
                // Resource object model.createResource(value.getAsString()).addProperty(XSD.xstring)
                // resource.addProperty(property, value.getAsString());
            } else if (value.isJsonArray()) {

                JsonArray array = value.getAsJsonArray();
                for (JsonElement element : array) {
                    if (element.isJsonPrimitive()) {
                        String[] splitValues = element.getAsString().split("-");
                        for (String splitValue : splitValues) {
                            // Create a triple for each part of the split string
                            Literal splitLiteral = model.createTypedLiteral(splitValue.trim(), XSD.xstring.getURI());
                            model.add(property, RDF.type, OWL.DatatypeProperty);
                            resource.addProperty(property, splitLiteral);
                            model.add(resource, property, splitLiteral);
                        }
                    }else if (element.isJsonObject()) {
                            handleStixObject(element.getAsJsonObject(), model);
                        }
                    }

            } else if (value.isJsonObject()) {

                JsonObject valueObject = value.getAsJsonObject();
                if (valueObject.has("id")) {
                    System.out.println(valueObject.getAsString());
                    String referencedId = getAsString(valueObject, "id");
                    Resource referencedResource = model.createResource(UCO_NAMESPACE + referencedId);
                    resource.addProperty(property, referencedResource);
                    handleStixObject(valueObject, model);
                }
            }
        }

        private static void handleReferences (JsonObject stixObject, Resource resource, Model model){
            if (stixObject.has("relationship_type") && stixObject.has("source_ref") && stixObject.has("target_ref")) {
                String relationshipType = mapStixToUcoRelationship(getAsString(stixObject, "relationship_type"));
                String sourceRef = getAsString(stixObject, "source_ref");
                String targetRef = getAsString(stixObject, "target_ref");

                Resource sourceResource = resources.get(sourceRef);
                Resource targetResource = resources.get(targetRef);

                if (sourceResource != null && targetResource != null) {
                    sourceResource.addProperty(ResourceFactory.createProperty(UCO_NAMESPACE, relationshipType), targetResource);
                    if (mapStixToUcoReverseProperty(relationshipType).equals("no_change")) {
                        targetResource.addProperty(ResourceFactory.createProperty(UCO_NAMESPACE, "linkedFrom" + relationshipType), sourceResource);
                    } else {
                        targetResource.addProperty(ResourceFactory.createProperty(UCO_NAMESPACE,  mapStixToUcoReverseProperty(relationshipType)), sourceResource);
                        model.createResource(UCO_NAMESPACE + mapStixToUcoReverseProperty(relationshipType)).addProperty(RDF.type, OWL.ObjectProperty);
                    }
                }
            }

            for (Map.Entry<String, JsonElement> entry : stixObject.entrySet()) {
                String key = entry.getKey();
                if (key.endsWith("_ref") && entry.getValue().isJsonPrimitive()) {
                    String referencedId = entry.getValue().getAsString();
                    Resource referencedResource = resources.get(referencedId);
                    if (referencedResource != null) {
                        String propertyName =mapStixToUcoProperty(key);
                        propertyName=propertyName.replace("_ref", "");
                        resource.addProperty(ResourceFactory.createProperty(UCO_NAMESPACE, propertyName), referencedResource);
                        model.add(ResourceFactory.createProperty(UCO_NAMESPACE,propertyName),RDF.type, OWL.ObjectProperty);
                        if(mapStixToUcoReverseProperty(propertyName).equals("no_change"))
                        {
                            referencedResource.addProperty(ResourceFactory.createProperty(UCO_NAMESPACE, "linkedFrom" + propertyName), resource);
                        }
                        else {
                            referencedResource.addProperty(ResourceFactory.createProperty(UCO_NAMESPACE,  mapStixToUcoReverseProperty(propertyName)),resource);
                        }
                    }
                } else if (key.endsWith("_refs") && entry.getValue().isJsonArray()) {
                    JsonArray refsArray = entry.getValue().getAsJsonArray();
                    for (JsonElement refElement : refsArray) {
                        if (refElement.isJsonPrimitive()) {
                            String referencedId = refElement.getAsString();
                            Resource referencedResource = resources.get(referencedId);
                            if (referencedResource != null) {
                                String propertyName = key.replace("_refs", "");
                                resource.addProperty(ResourceFactory.createProperty(UCO_NAMESPACE, propertyName), referencedResource);
                                     if(mapStixToUcoReverseProperty(propertyName).equals("no_change"))
                                {
                                    referencedResource.addProperty(ResourceFactory.createProperty(UCO_NAMESPACE, "linkedFrom" + propertyName), resource);
                                }
                                  else {
                                         referencedResource.addProperty(ResourceFactory.createProperty(UCO_NAMESPACE,  mapStixToUcoReverseProperty(propertyName)),resource);
                                     }
                            }
                        }
                    }
                }
            }
        }

        private static String getAsString (JsonObject jsonObject, String key){
            JsonElement element = jsonObject.get(key);
            return (element != null && !element.isJsonNull()) ? element.getAsString() : null;
        }

        private static String mapStixToUcoProperty (String stixProperty){
            // Map STIX properties to UCO properties as needed
            switch (stixProperty) {
                case "created":
                    return "created";
                case "modified":
                    return "modified";
                case "name":
                    return "name";
                case "description":
                    return "description";
                case "labels":
                    return "labels";
                case "pattern":
                    return "pattern";
                case "valid_from":
                    return "validFrom";
                case "id":
                    return "id";
                case "first_seen":
                    return "firstSeen";
                case "last_seen":
                    return "lastSeen";
                case "spec_version":
                    return "specVersion";
                case "sectors":
                    return "sector";
                case "aliases":
                    return "alias";
                case "objective":
                    return "goals";
                case "identity_class":
                    return "identityClass";
                case "is_family":
                    return "isFamily";
                case "number_observed":
                    return "value";
                case "first_observed":
                    return "firstObserved";
                case "last_observed":
                    return "lastObserved";
                case "opinion":
                    return "opinion";
                case "published":
                    return "published";
                case "sophistication":
                    return "skillLevel";
                case "pattern_type":
                    return "patternType";
                case "revoked":
                    return "revoked";
                case "country":
                    return "country";
                case "relationship_type":
                    return "relationshipType";
                case "sighting":
                    return "Sighting";
                case"definition_type":
                    return"definitionType";
                case"object_marking_refs":
                    return"objectMarking";
                case"threat_actor_types"  :
                     return "label";
                case"primary_motivation":
                    return"primaryMotivation";
                case"malware_types":
                    return"malwareTypes";
                case"indicator_types":
                    return "indicatorTypes";
                case"kill_chain_phases":
                     return "killChainPhase";
                case"sighting_of_ref":
                    return"sightingOf";
                case"created_by_ref":
                    return "createdBy";
                case"observed_data_refs":
                    return"observed";
                case"resource_level":
                    return"recourseLevel";
                case"contact_information":
                    return"contactInformation";
                // Add more mappings as needed
                default:
                    return stixProperty;
            }
        }
    private static String mapStixToUcoReverseProperty (String stixProperty){
        switch (stixProperty) {
            case"uses":
                return"usedBy";
            case"where_sighted":
                return"sightedBy";
            case"source":
                return "sourceOf";
            case"target":
                return "targetOf";
            case"attributedTo"  :
                return"attributionOf" ;
            case"createdBy":
                return "creatorOf";
            case"delivers":
                return"deliveredFrom" ;
            case"exploits":
                return "exploitedBy";
            case"indicates":
                return "indicatedBy";
            case"mitigates":
                return "mitigatedBy";
            default:
                return "no_change";
        }

    }
        private static String mapStixToUcoRelationship (String stixRelationship){
            // Map STIX relationships to UCO relationships as needed
            switch (stixRelationship) {
                case "uses":
                    return "uses";
                case "targets":
                    return "targets";
                case "indicates":
                    return "indicates";
                case "mitigates":
                    return "mitigates";
                case "attributed-to":
                    return "attributedTo";
                case "exploits":
                    return "exploits";
                case "variant-of":
                    return "variant";
                case "authored-by":
                    return "authoredBy";
                case "impersonates":
                    return "impersonates";
                case "duplicate-of":
                    return "duplicateOf";
                case "delivered-from":
                    return "deliveredFrom";
                case "realated-to":
                    return "relatedTo";
                case "based-on":
                    return "basedOn";
                case "lacated-at":
                    return "locatedAt";
                case "delivers":
                    return "delivers";


                // Add more mappings as needed
                default:
                    return stixRelationship;
            }
        }

        private static void storeRdfInGraphDb (Model model, String graphDbUrl, String repositoryId, String
        username, String password){
            HTTPRepository repository = new HTTPRepository(graphDbUrl + "/repositories/" + repositoryId);
            repository.setUsernameAndPassword(username, password);
            repository.initialize();

            try (RepositoryConnection conn = repository.getConnection()) {
                StringWriter writer = new StringWriter();
                model.write(writer, "TTL", "showDataTypes");
                InputStream rdfInputStream = new ByteArrayInputStream(writer.toString().getBytes(StandardCharsets.UTF_8));
                conn.add(rdfInputStream, "", RDFFormat.TURTLE);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                repository.shutDown();
            }
        }
        private static String ucoclass (String type)
        {
            char[] str;
            str = type.toCharArray();
            char p = Character.toUpperCase(str[0]);
            str[0] = p;
            if (type.contains("-")) {
                for (int i = 0; i < str.length; i++) {
                    if (str[i] == '-') {
                        for (int j = i; j < str.length - 1; j++) {
                            if (i == j)
                                str[i] = Character.toUpperCase(str[i + 1]);
                            else
                                str[j] = str[j + 1];
                            //str[str.length-1]=' ';
                        }
                        char[] newstr = new char[str.length - 1];
                        for (int j = 0; j < str.length - 1; j++) {
                            newstr[j] = str[j];
                        }
                        return new String(newstr);
                    }
                }
            }

            return new String(str);
        }
        private static final String[] DATE_FORMATS = {
                "yyyy-MM-dd", "dd-MM-yyyy", "MM/dd/yyyy", "yyyy/MM/dd"
        };
        public static String detectLiteralType (String nameLiteral){
            // Check if it's an integer
            if (isInteger(nameLiteral)) {
                return "Integer";
            }

            // Check if it's a date
            if (isDate(nameLiteral)) {
                return "Date";
            }
            if (isFloat(nameLiteral)) {
                return "Float";
            }
            if (isBoolean(nameLiteral)) {
                return "Boolean";
            }
            // Otherwise, assume it's a plain string
            return "String";
        }

        // Helper method to check if a string is an integer
        public static boolean isInteger (String input){
            try {
                Integer.parseInt(input);
                return true;
            } catch (NumberFormatException e) {
                return false;
            }
        }

        // Helper method to check if a string is a date (using multiple date formats)
        public static boolean isDate (String input){
            for (String format : DATE_FORMATS) {
                if (isValidFormat(format, input)) {
                    return true;
                }
            }
            return false;
        }
        public static boolean isBoolean (String input){
            return input.equalsIgnoreCase("true") || input.equalsIgnoreCase("false");
        }
        // Helper method to validate the date format
        private static boolean isValidFormat (String format, String value){
            SimpleDateFormat sdf = new SimpleDateFormat(format);
            sdf.setLenient(false);
            try {
                sdf.parse(value);
                return true;
            } catch (ParseException e) {
                return false;
            }
        }
        public static boolean isFloat (String input){
            try {
                Float.parseFloat(input);
                return true;
            } catch (NumberFormatException e) {
                return false;
            }
        }

    private static void handleExternalReferences(JsonArray externalReferences, Resource resource, Model model) {
        for (JsonElement refElement : externalReferences) {
            JsonObject externalRef = refElement.getAsJsonObject();

            // Create an RDF resource for each external reference
            String externalId = getAsString(externalRef, "external_id");
            String extenalname=getAsString(externalRef,"source_name");
            if(externalId==null)
            {
                externalId=extenalname.replace(" ","-");
            }
            if (externalId != null) {
                model.createResource(UCO_NAMESPACE+"ExternalReference").addProperty(RDF.type,RDFS.Class);
                Resource externalReferenceResource = model.createResource(UCO_NAMESPACE + "external-reference--" + externalId)
                        .addProperty(RDF.type, ResourceFactory.createResource(UCO_NAMESPACE + "ExternalReference"));

                if (externalRef.has("source_name")) {
                    externalReferenceResource.addProperty(ResourceFactory.createProperty(UCO_NAMESPACE, "sourceName"),
                            model.createTypedLiteral(externalRef.get("source_name").getAsString(), XSD.xstring.getURI()));
                }

                if (externalRef.has("url")) {
                    externalReferenceResource.addProperty(ResourceFactory.createProperty(UCO_NAMESPACE, "url"),
                            model.createTypedLiteral(externalRef.get("url").getAsString(), XSD.xstring.getURI()));
                }
                if (externalRef.has("description")) {
                    externalReferenceResource.addProperty(ResourceFactory.createProperty(UCO_NAMESPACE, "description"),
                            model.createTypedLiteral(externalRef.get("description").getAsString(), XSD.xstring.getURI()));
                }
                // Add the external reference to the STIX object (resource)
                resource.addProperty(ResourceFactory.createProperty(UCO_NAMESPACE, "hasExternalReference"), externalReferenceResource);
                model.createResource(UCO_NAMESPACE + "hasExternalReference").addProperty(RDF.type, OWL.ObjectProperty);
            }





        }
    }

}



