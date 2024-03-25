import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.io.BufferedReader;
import java.io.Console;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import org.json.JSONArray;
import org.json.JSONObject;


public class getOggReport {

	
	   static JSONObject jObj = null;
	   static Map<String, String> listProcess = new HashMap<String, String>();

	   static String currentDeploy;
	   static String currentHostname;
       static String username;
	   static String password;
	   static String oggUrl ;
	   static String oggHttp ="";
	   static String httpUrlService="";
	   static String deployName="";
	   static String defaultDir;
	   static String defaultProperties;
	   static String suffixeFile;
	   static String pathStart=""; 
	   static HttpURLConnection connectHttp = null ;  
	    
	   public static String getPropertiesOgg(){
		   return pathStart;
	   }
	   
	   
	   public static void main(String[]args) throws IOException, NumberFormatException, ParseException, SQLException, ClassNotFoundException
	   {
		   if (args.length == 0) {
		      Console console = System.console();
		   
			   System.out.println("Enter user and pwd for GoldenGate Service Manager:");
			   username = console.readLine("Username: ");
			   if (username.equals("")) {
		    	   System.out.println("Username is null, set default : oggadmin");
		    	   username="oggadmin";
		       }
			   
		       password = new String(console.readPassword("Password: "));
		       
		       if (password.equals("")) {
		    	   System.out.println("Password is null, set default : oggadmin");
		    	   password="oggadmin";
		       }
		       defaultProperties = console.readLine("Properties file: ");
		       
		   }
		   else {
			   username=args[0];
			   password=args[1];
			   defaultProperties=args[2];
		   }
	       
	       
	       if (defaultProperties.equals("")) {
	    	   System.out.println("Properties file is null, set default : oggConnect.properties");
	    	   defaultProperties="oggConnect";
	       }
		   
	       defaultDir = oggProperties.getString("defaultDir"); 
		   oggUrl = oggProperties.getString("oggUrl"); 
		   
		   File fileStructure = new File(defaultDir);
		   if(! fileStructure.exists()) {
			   if (fileStructure.mkdirs()) {
                   System.out.println(defaultDir + " created successfully.");
               } else {
                   System.out.println("Couldn't create " + fileStructure.getName());
                   System.exit(0);
               }
		   }
		   
		   new getOggReport().getOciOggInfo();
   }

     
   private void getOciOggInfo() throws IOException, NumberFormatException, ParseException, SQLException, ClassNotFoundException{
	   suffixeFile= new SimpleDateFormat("yyyy_MM_dd_H_m_s",   Locale.getDefault()).format(new Date());
	   System.setProperty("https.protocols", "TLSv1,TLSv1.1,TLSv1.2");
      oggHttp=oggUrl+"/services/v2/deployments";
	  JSONObject result = httpOgg(oggHttp,username,password,"GET","");
      Map<String, String> deployList = getDeployment(result);
      for (Map.Entry<String, String> entry : deployList.entrySet()) {
    	 String depName=entry.getKey();
         oggHttp=oggUrl+"/services/v2/deployments/"+depName;
	     result = httpOgg(oggHttp,username,password,"GET","");
	   }
      connectHttp.disconnect();
      System.out.println("\n\rResult into :"+defaultDir+File.separator+"OGG_REPORT_"+suffixeFile);
    }

   private static void getRetrieveServices(String depName, JSONObject ojb) throws IOException, ParseException 
   {
	   String host=null;
	   JSONObject result = (JSONObject) jObj.get("response");
	   JSONArray getInput = (JSONArray) result.get("items");
	   for (int i = 0; i < getInput.length(); i++) {
    	   	String link=null;
    	   	ArrayList<String> res=   new ArrayList<String>();
    	    JSONObject getService = getInput.getJSONObject(i);
    	    String name = (String) getService.get("name");
    	    
    	    
    	    JSONArray getLinks = (JSONArray) getService.get("links");
  	    	for (int j = 0; j < getLinks.length(); j++) {
  	    		JSONObject currentLink = getLinks.getJSONObject(j);
  	    		String depHref=currentLink.get("href").toString();
  	
  	    		if (depHref.contains(depName)) {
  	    			String urlServices=currentLink.get("href").toString()+"/services";
  	    			res.add(urlServices);
  	    			link = urlServices=currentLink.get("href").toString();
  	    		}
  	    	}
    	    
    	    
    	    oggHttp=oggUrl+"/services/v2/deployments/"+depName+"/services/"+name;
	   	    JSONObject resultServices = httpOgg(oggHttp,username,password,"GET","");
	   	    JSONObject getresultService = (JSONObject) resultServices.get("response");
	   	    JSONObject getConfig = (JSONObject) getresultService.get("config");
	   	    JSONObject getNetwork= (JSONObject) getConfig.get("network");
	 	   	String port = getNetwork.get("serviceListeningPort").toString();
	   	    host =link.split("//")[1].split(":")[0];
	   	    
	  	  
	 	    if (name.contains("adminsrvr")) {
	 		  httpUrlService="http://"+host+":"+port+"/services/v2/";
	 		  result = httpOgg(httpUrlService+"extracts",username,password,"GET","");
	 		  try{
	 			  getlistProcess(result,"EXTRACT"); 
	 		  }
	 		  catch(Exception e) {e.getMessage();}
	 		  
	 		  result = httpOgg(httpUrlService+"replicats",username,password,"GET","");
	 		  try {
	 			  getlistProcess(result,"REPLICAT"); 
	 		  }
	 		  catch(Exception e) {}
 		  
	 	  }
       }    
   }
   
	  
   
   
   private static Map<String, String> getDeployment(JSONObject ojb) throws IOException, ParseException 
   {
	   Map<String, String> dictionary = new HashMap<String, String>();
	   JSONObject result = (JSONObject) jObj.get("response");
	   JSONArray getInput = (JSONArray) result.get("items");
       for (int i = 0; i < getInput.length(); i++) {
    	    ArrayList<String> res=   new ArrayList<String>();
    	    JSONObject currentChk = getInput.getJSONObject(i);
    		String depName= currentChk.get("name").toString();
    		if (!depName.contains("ServiceManager")) {
  	    		deployName=depName;
  	    		currentDeploy=depName;
  	    		oggHttp=oggUrl+"/services/v2/deployments/"+depName+"/services";
  	   	     	result = httpOgg(oggHttp,username,password,"GET","");
  	   	     	getRetrieveServices(depName,result);
    		}
  	    	dictionary.put(depName, String.join(";", res));
  	   }
    return dictionary;
    
   }
  
   
  
   
   private static JSONObject httpOgg(String oggHttp, String username, String password,String method,String params) throws IOException {
	   URL url;   
	   jObj = null;
	   url = new URL(oggHttp); 
	   String userpass = username + ":" + password;
       String basicAuth = "Basic " + javax.xml.bind.DatatypeConverter.printBase64Binary(userpass.getBytes());
	   connectHttp = (HttpURLConnection) url.openConnection();
       connectHttp.setRequestProperty ("Authorization", basicAuth);
       connectHttp.setRequestMethod(method);
       connectHttp.setRequestProperty("accept", "application/json");
       connectHttp.setConnectTimeout(10000);
      
       try{
    	   jObj=gethttpOgg(method,params);
       }
       catch (Exception e) {}
       connectHttp.disconnect();
       return jObj;
   }
       
	private static JSONObject gethttpOgg (String method,String params) throws IOException {  
			JSONObject jObj = null;
			if (method.equals("GET")){
			   try {
		          if(connectHttp!=null){
		 		    try {
		 		       BufferedReader br =new BufferedReader(new InputStreamReader(connectHttp.getInputStream()));
		 		       String input;
		 		       while ((input = br.readLine()) != null){
		 		           jObj = new JSONObject(input);
		 		           }
		 		       br.close();
		 		    } catch (Exception e) {
		 		    }
		         }
		        } catch (Exception e) {
		         e.printStackTrace();}
		   }
			
			
		
		   return jObj; 
   }

   


	
   
   private static void getlistProcess(JSONObject ojb,String type) throws IOException, ParseException, SQLException 
   {
	 
	   JSONObject getProcess = (JSONObject) jObj.get("response");
       org.json.JSONArray listExtract = getProcess.getJSONArray("items");
       for (int i = 0; i < listExtract.length(); i++) {
   	    	JSONObject extractName = listExtract.getJSONObject(i);
   	    	Object name = extractName.get("name");
   	    	if (type.equals("EXTRACT")) {
   	    		System.out.println("\r\n");
   	    		listProcess.put(name.toString(), "EXTRACTS");
   	    		JSONObject result = httpOgg(httpUrlService+"extracts/"+name.toString()+"/info/reports",username,password,"GET","");
   	    		JSONObject getProcessDet = (JSONObject) result.get("response");
   	    		
   	    		JSONArray getInput = (JSONArray) getProcessDet.get("items");
   	    		System.out.println("Deploy " + deployName+" Extract :");
	    			
   	    		for (i = 0; i < getInput.length(); i++) {
   	    			new ArrayList<String>();
   	    			JSONObject currentChk = getInput.getJSONObject(i);
   	    			String reportName= currentChk.get("name").toString();
   	    			JSONObject resReport = httpOgg(httpUrlService+"extracts/"+name.toString()+"/info/reports/"+reportName,username,password,"GET","");
   	    			getReport(resReport,reportName);
   	    		};
   	    	
   	    		}
   	    	
   	    	else {
   	    		listProcess.put(name.toString(), "REPLICATS");
   	    		JSONObject result = httpOgg(httpUrlService+"replicats/"+name.toString()+"/info/reports",username,password,"GET","");
   	    		JSONObject getProcessDet = (JSONObject) result.get("response");
   	    		System.out.println("\r\nDeploy " + deployName+" Replicat :");
	    		
   	    		JSONArray getInput = (JSONArray) getProcessDet.get("items");
   	    		for (i = 0; i < getInput.length(); i++) {
   	    			new ArrayList<String>();
   	    			JSONObject currentChk = getInput.getJSONObject(i);
   	    			String reportName= currentChk.get("name").toString();
   	    			JSONObject resReport = httpOgg(httpUrlService+"replicats/"+name.toString()+"/info/reports/"+reportName,username,password,"GET","");
   	    			getReport(resReport,reportName);
   	    		};
   	    	}
   	   }
   }
   

   
   private static void getReport(JSONObject jObj,String report) throws IOException, ParseException, SQLException 
   {
	   
	   String deployDir = defaultDir+File.separator+"OGG_REPORT_"+suffixeFile+File.separator+deployName;
	   File fileStructure = new File(deployDir);
	   if(! fileStructure.exists()) {
		   if (fileStructure.mkdirs()) {
             System.out.println("\t"+deployDir + " created successfully.");
         } else {}
	   }
	   System.out.println("\t\tFile:"+report);
	   
	   String pathServices= String.format( "%s%s%s",deployDir,File.separator,report);
	   FileWriter fw = new FileWriter(pathServices, true);
		
	   JSONObject getProcess = (JSONObject) jObj.get("response");
	   JSONArray  listExtract = getProcess.getJSONArray("lines");
	   Iterator<?> i = listExtract.iterator();
	   while (i.hasNext()) {
		   fw.write(i.next().toString()+"\n\r");
	   }
	   fw.close();
   }
   

}