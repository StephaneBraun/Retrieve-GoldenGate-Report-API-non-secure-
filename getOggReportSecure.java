import java.io.BufferedReader;
import java.awt.Desktop;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFTable;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTTable;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTTableStyleInfo;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.ss.util.AreaReference;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTTableColumn;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTTableColumns;

import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.logging.Level;



/**
* based on http://stackoverflow.com/a/559146/1313040
* also based on http://stackoverflow.com/a/2922031/1313040
*/
public class Tail implements Runnable {

    private long _updateInterval = 2000;
    private long _filePointer;
    static int totalRows;
    static int totalElapse;
    
    private File _file;
    private static volatile boolean keepRunning = true;
    private static String process;
    private static String path;
    
    static int intervalInMinutes;
    static Row row ;
    static Cell cell;
    static CellStyle cellStyleCenter ;
    static SimpleDateFormat formatterDate ;
    private static Logger logger = null;
    static Tail tail ;
    
    public static void main(String[] args) throws IOException, NumberFormatException, ParseException {
        final Thread mainThread = Thread.currentThread();
        path=args[0];
        process=args[1];   
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                keepRunning = false;
                try {
                    mainThread.join();
                } catch (InterruptedException ex) {
                    Logger.getLogger(Tail.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        });
        String reportFile=path+"/"+process+".rpt";
        System.out.println("Report file:"+reportFile);
        File log = new File(path+"/"+process+".rpt");
        tail = new Tail(log);
        
       new Thread(tail).start();

    }

    public Tail(File file) {
        this._file = file;
    }

    @Override
    public void run() {
        try {

            while (keepRunning) {
            	
            	Process process = Runtime.getRuntime().exec("ps -e --sort=-pcpu -o pcpu,size,flags,lim,lstart,nice,rss,start,state,tt,wchan,command");
    	        BufferedReader r = new BufferedReader(new InputStreamReader(process.getInputStream()));
    	        String res = null;
    	        String search=Tail.process+".rpt";
    	        String lineProc;
    	        while ((lineProc = r.readLine()) != null) {
			    	if (lineProc.contains(search)) {
    	        		res=lineProc;}
    	        }
    	        if (res.length()<0) {
    	        	System.out.println("End....");
    	        	//System.exit(0);
    	        }
                Thread.sleep(_updateInterval);
                long len = _file.length();

                if (len < _filePointer) {
                    // Log must have been jibbled or deleted.
                    this.appendMessage("Log file was reset. Restarting logging from start of file.");
                    _filePointer = len;
                } else if (len > _filePointer) {
                    // File must have had something added to it!
                    RandomAccessFile raf = new RandomAccessFile(_file, "r");
                    raf.seek(_filePointer);
                    String line = null;
                    while ((line = raf.readLine()) != null) {
                        this.appendLine(line);
                    }
                    _filePointer = raf.getFilePointer();
                    raf.close();
                }
            }
        } catch (Exception e) {
            this.appendMessage("GoldenGate processus "+process + " not runninge");
            
            if (!Thread.interrupted()) {
             	Thread.currentThread().interrupt();
             }
            
            try {
				//analyseReportFile.getReport(path,process);checkRate(path);
            	checkRate(path);
               
	        	
			} catch (NumberFormatException | IOException | ParseException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
        }
        // dispose();
    }

    private void appendMessage(String line) {
        System.out.println(line.trim());
    }

    private void appendLine(String line) {
        System.out.println(line.trim());
    }
    
    
    
	static long elapseTime(String datePrec, String dateCrt) throws ParseException {
		long resultTimeSec = 0;
		try {
		SimpleDateFormat formatterDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.FRENCH);
		Date dateDeb = formatterDate.parse(datePrec);
		Date dateEnd = formatterDate.parse(dateCrt);
		TimeUnit timeUnitSec =  TimeUnit.SECONDS;
		long diffInMillies = (dateEnd.getTime() - dateDeb.getTime()) ;
		resultTimeSec = timeUnitSec.convert(diffInMillies,TimeUnit.MILLISECONDS);
		}
		catch(Exception e) {}
		return resultTimeSec ;
		}
	
	
	static void checkRate(String path) throws IOException, ParseException, NumberFormatException {
		  path=path+"/";
		  System.out.println("Starting analysis:");
		  BufferedWriter writer =null;
		  File f = new File(path); 
		  File[] files = f.listFiles(); 
		  
		  HashMap<String, List<Object>> initialTable = new HashMap<>();
		  HashMap<String, String> initialTableRows = new HashMap<>();
		  HashMap<String, List<Object>> finalTable = new HashMap<>();  
		  
		for (int i = 0; i < files.length; i++) {
			if (files[i].getName().indexOf(process+".rpt")>=0)
			{
				
				System.out.println("Goldengate report file: "+files[i].getName());
				try {
					writer = new BufferedWriter(new FileWriter(path+files[i].getName().replace(".rpt",".txt")));
				} catch (Exception e1) {
					System.out.println("Create text file failed.");
					System.out.println(e1.getMessage());
					e1.printStackTrace();
					System.exit(0);
				}
				files[i].getName().replace(".rpt","");
				
				System.out.println("Generate flat file: "+path+files[i].getName().replace(".rpt",".txt"));
	    		Timestamp timestamp = new Timestamp(System.currentTimeMillis());
	    		String currentTimestamp = Long.toString(timestamp.getTime());

	    		String excelFilexlsx=path+files[i].getName().replace(".rpt","_"+currentTimestamp+".xlsx");
	    	  	
	    		@SuppressWarnings("resource")
				BufferedReader br = new BufferedReader(new FileReader(path+files[i].getName())); 
				String line = br.readLine();
				String datePrec=null;
				String dateCrt=null;
				String tablePrec=null;
				int cpt=0;
				while ( line != null) 
				{		
					if (line.contains("  INFO    OGG-02911  Processing table ")) {
							String res = line.split("  INFO    OGG-02911  Processing table ")[1];
							dateCrt = line.split("  INFO    OGG-02911  Processing table ")[0];
							StringBuffer sb= new StringBuffer(res);  	
							String tableCrt=sb.deleteCharAt(sb.length()-1).toString();
							br.readLine();
							if (cpt>0) {
								//System.out.println("Start :"+ datePrec + " -- "+dateCrt);
								long elapseSec = 1;
							    elapseSec = elapseTime(datePrec,dateCrt);
								if (elapseSec<1) {elapseSec=1;}
								totalElapse+=elapseSec;
								initialTable.put(tablePrec, Arrays.asList(datePrec,dateCrt,elapseSec));
			    		        
							}
							datePrec=dateCrt;
							tablePrec=tableCrt;
							cpt++;
					}
					
					if (line.contains("Report at ")) {
						dateCrt = line.replace("Report at ","").split(" \\(")[0];
						long elapseSec = 1;
						elapseSec = elapseTime(datePrec,dateCrt);
						
						if (elapseSec<1) {elapseSec=1;}
						totalElapse+=elapseSec;
						initialTable.put(tablePrec, Arrays.asList(datePrec,dateCrt,elapseSec));

					}
					
					
					if (line.contains("From table ")) {
						String res= line.replace("From table ","");
						StringBuffer sb= new StringBuffer(res);  	
						String tableCrt=sb.deleteCharAt(sb.length()-1).toString();
						String nbRows= br.readLine();
						nbRows=nbRows.split(":")[1].replace(" ","");
						totalRows+= Integer.valueOf(nbRows);
						initialTableRows.put(tableCrt ,nbRows);

					}
					line = br.readLine();
				}
			    	
				SortedSet<String> keyTable = null ;
				//try {
					keyTable = new TreeSet<>(initialTable.keySet());
					for (String key : keyTable) { 
	    		    	List<Object> res= initialTable.get(key);
	    		    	
	    		    	long nbRows =Long.parseLong (initialTableRows.get(key).toString());
	    		    	long elapse= Long.parseLong (res.get(2).toString());
	    		    	long average= nbRows/elapse;
	    		    	System.out.println(key + "-> from "+ res.get(0) + " to "+ res.get(1) + " - Nb rows: "+ nbRows + " - Elapse:"+elapse+ " - Average:"+average);
	    		    	finalTable.put(key, Arrays.asList(res.get(0),res.get(1),nbRows,elapse,average));
	    		    }
		
	    		    String result="";
		    	    writer.write("tablename;start;end;row;elapse;average;\n");
		    	    SortedSet<String> keys = new TreeSet<>(finalTable.keySet());
		    	    for (String key : keys) { 
		    	    	//System.out.println("Key :"+key + " Value:"+dictionary.get(key).replace(" ","")+"-");
		    	    	List<Object> valueRows = finalTable.get(key);
		    	    	String res="";
		    	    	for (int x = 0; x < valueRows.size(); x++) {
		    	    	    res += valueRows.get(x)+";";
		    	    	}
		    	    	
		    	    	BigDecimal divide = new BigDecimal((Double.valueOf(valueRows.get(2).toString())/totalRows)*100.0);
					    double pourcent = divide.setScale(2, BigDecimal.ROUND_HALF_EVEN).doubleValue();
					    res += pourcent+";";
					    divide = new BigDecimal((Double.valueOf(valueRows.get(3).toString())/totalElapse)*100.0);
					    pourcent = divide.setScale(2, BigDecimal.ROUND_HALF_EVEN).doubleValue();
					    res += pourcent+";";					    
		    	        result=key + ";" + res+"\n";
		    	        writer.write(result);
		    	  
			        }
	
		    	    writer.close();		
		    	    
		        	@SuppressWarnings("resource")
		    	  	XSSFWorkbook wb = new XSSFWorkbook();
		    	  	String sheet = "Initial Load Statistics";
		    	  	XSSFSheet sheetTime = wb.createSheet(sheet);
		
		    	  	// Date format
		    	  	CellStyle cellStyleDate = wb.createCellStyle();  
		    	  	CreationHelper createHelper = wb.getCreationHelper();  
		    	  	cellStyleDate.setDataFormat(createHelper.createDataFormat().getFormat("YYYY/mm/dd hh:mm:ss"));  
		    		
		    	  	DataFormat formatNumber = wb.createDataFormat();
		    	  	XSSFCellStyle cellStyleNumber = wb.createCellStyle();
		    	  	cellStyleNumber.setDataFormat(formatNumber.getFormat("### ### ### ##0")); // custom number format
	
		    	  	cellStyleCenter = wb.createCellStyle();
		    	  	cellStyleCenter.setVerticalAlignment(VerticalAlignment.CENTER);
		    	  	cellStyleCenter.setAlignment(HorizontalAlignment.CENTER);
		    	       
		    	  	List<String> header = Arrays.asList("Tablename","Start","End","Rows","Elapse in sec","Average rows/sec","% rows","% elapse");   
		    	    row = sheetTime.createRow((short) 0);
		    	    addHeader(header);
		    	  
		    	    sheetTime.createFreezePane(0, 1);
		    	    excelFilexlsx=path+files[i].getName().replace("rpt","xls");
			        int rowPos=1;
	    	        new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.FRENCH);
	    	        HashMap<Integer, List<Object>> data = new HashMap<>();
	    	    	
	    	        for (String key : keys) { 
	    	        	row = sheetTime.createRow(rowPos);
	    	        	data = new HashMap<>();
		    	    	List<Object> valueRows = finalTable.get(key);
		    	    	data.put(0, Arrays.asList(key.toString(), null,"STRING"));
		    	 		for (int x = 0; x < valueRows.size(); x++) {
		    	 			if (x>=0 || x<2) {
		    	    			data.put(x+1, Arrays.asList(valueRows.get(x), cellStyleDate,"DATE"));		
		    	    		}
		    	    		if (x>=2 ) {
		    	    			data.put(x+1, Arrays.asList(valueRows.get(x), cellStyleNumber,"NUMBER"));		
		    	    		}
	    	    	        addCell(data);
		    		    }
		    	 		
		    	 		
		    	 		BigDecimal divide = new BigDecimal((Double.valueOf(valueRows.get(2).toString())/totalRows)*100.0);
					    double pourcent = divide.setScale(2, BigDecimal.ROUND_HALF_EVEN).doubleValue();
					    data.put(6, Arrays.asList(pourcent, cellStyleCenter,"STRING"));
					    addCell(data);
					    
					    divide = new BigDecimal((Double.valueOf(valueRows.get(3).toString())/totalElapse)*100.0);
					    pourcent = divide.setScale(2, BigDecimal.ROUND_HALF_EVEN).doubleValue();
					    data.put(7, Arrays.asList(pourcent, cellStyleCenter,"STRING"));
					    addCell(data);
					    
					    rowPos=rowPos+1;
	    	        }
	    	        
	    	        row = sheetTime.createRow(rowPos);
	    	        data = new HashMap<>();
	    	        data.put(0, Arrays.asList("Total", null,"STRING"));
	    			data.put(3, Arrays.asList(totalRows, cellStyleNumber,"NUMBER"));		
	    			data.put(4, Arrays.asList(totalElapse, cellStyleNumber,"NUMBER"));
	    			data.put(5, Arrays.asList(totalRows/totalElapse, cellStyleNumber,"NUMBER"));
	    			data.put(6, Arrays.asList(100, cellStyleCenter,"NUMBER"));
	    			data.put(7, Arrays.asList(100, cellStyleCenter,"NUMBER"));
	    	    	addCell(data);
	    	        
	    	        if (rowPos>1) {
		    	        @SuppressWarnings("deprecation")
						XSSFTable formatTime = sheetTime.createTable();    
		    	        CTTable cttableTime = formatTime.getCTTable();
		    	        formatTable(sheetTime,cttableTime,"Time",rowPos,8,1);
		    	        formatTime.updateReferences();
		    	    }
	    	        
	    	        for (int ii = 0; ii <= 7; ii++) {
	    	      	  sheetTime.autoSizeColumn(ii);
	    	      	}    
	
	    	        excelFilexlsx=path+files[i].getName().replace("rpt","xlsx");
	    	        System.out.println("Rows: "+totalRows + " elapse "+totalElapse);
	    	    	FileOutputStream fileOut = new FileOutputStream(excelFilexlsx);
	    	    	wb.write(fileOut);
	    	    	try {
	    	    		System.out.println("Excel file: "+excelFilexlsx);
	    	    		Desktop.getDesktop().open(new File(excelFilexlsx));
	    	    	}
	    	    	catch(Exception e) {}
			
    		    
    	    	
			}
		}
	}
		
	
	
    private static void addCell(HashMap<Integer, List<Object>> data) throws ParseException 
	   {
		   
		   for (Entry<Integer, List<Object>> entry : data.entrySet()) {
			    Integer key = entry.getKey();
			    List<Object> value = entry.getValue();
			    CellStyle  style = (CellStyle)  value.get(1);
			    cell = row.createCell(key);
			    if (value.get(2).equals("DATE")){
			    	String date = value.get(0).toString();
			    	cell.setCellValue(date);  
			    }
			    else if (value.get(2).equals("STRING")){
			    	String str= value.get(0).toString();
			    	cell.setCellValue(str);  
			    }
			    else if (value.get(2).equals("NUMBER")){
			    	int  integer = Integer.valueOf(value.get(0).toString());
			    	cell.setCellValue(integer);  
			    }
			    
		        if (style != null) {
		        	cell.setCellStyle(style);  
		        }
		   }  
	   }

	   
	   private static void formatTable(XSSFSheet sheet,CTTable cttable, String tableName,int rows,int cols,int numberFormat) 
	   {
		   CTTableStyleInfo table_style = cttable.addNewTableStyleInfo();
	       table_style.setName("TableStyleMedium9");           
	       table_style.setShowColumnStripes(false); //showColumnStripes=0
	       table_style.setShowRowStripes(true); //showRowStripes=1    
	       AreaReference my_data_range = new AreaReference(new CellReference(0, 0), new CellReference(rows, cols-1), null);    
	       cttable.setRef(my_data_range.formatAsString());
	       cttable.setDisplayName(tableName);      /* this is the display name of the table */
	       cttable.setName(tableName);    /* This maps to "displayName" attribute in &lt;table&gt;, OOXML */            
	       cttable.setId(numberFormat); //id attribute against table as long value
	       CTTableColumns columns = cttable.addNewTableColumns();
	       long longCol = cols;
	       columns.setCount(longCol);
	        for (int ii = 0; ii < cols; ii++)
	        {
	        	CTTableColumn column = columns.addNewTableColumn();   
	        	column.setName("Column" + ii);      
	            column.setId(ii+1);
	        };   
	    }
	   
	   
	   
	   private static void addHeader(List<String> header) 
	   {
		   for (short i = 0; i < header.size(); i++) {
		   	  cell = row.createCell(i);
		      cell.setCellValue(header.get(i));
		      cell.setCellStyle(cellStyleCenter);
		   }
	   }

	   
	   
}