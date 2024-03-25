# Retreive-GoldenGate-Report-and-Discard-files-for-Non-Secure-Deployment
Retreive GoldenGate Report and Discard files through API's for non secure deployment 

This utility allows you to download the report and discard files of GoldenGate deployments from the connection to a Service Manager.
It works with GoldenGate Microservices Deployments in insecure mode.

Prerequisites:
- JDK 1.8 minimum
- Have a properties file in the jar launch directory. Example oggConnect.properties

             oggUrl=http://ip or alias:service manager port
             defaultDir=/app/temp
 
This file contains 2 parameters:
- GoldenGate service manager Url connexion
- The output directory for receiving report and discard files

    It is possible to have several properties files in the installation directory with different names. The name of the properties file is part of the utility's startup parameters. Example:

       europeOggDeploy.properties
       usOggDeploy.properties
  

Launching the utility: 2 possible methods


1 - With the user parameters, GoldenGate password of the Service Manager and properties file as parameters:


            java -jar getOggReport.jar userOgg passwordOgg oggConnect


2 - With prompt command for the user, GoldenGate password from the Service Manager and properties file:

           java -jar getOggReport.jar



The utility will automatically create under the directory declared via the defaultDir variable a subdirectory OGG_REPORT_yyyy_mm_dd_hh_mi_ss, and subdirectories corresponding to the name of the GoldenGate deployment
