import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map.Entry;

import javax.xml.xpath.XPath;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.NicelyResynchronizingAjaxController;
import com.gargoylesoftware.htmlunit.ScriptResult;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebWindow;
import com.gargoylesoftware.htmlunit.html.DomAttr;
import com.gargoylesoftware.htmlunit.html.DomNode;
import com.gargoylesoftware.htmlunit.html.DomNodeList;
import com.gargoylesoftware.htmlunit.html.HtmlDivision;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlFieldSet;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlInput;
import com.gargoylesoftware.htmlunit.html.HtmlOption;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlSelect;
import com.gargoylesoftware.htmlunit.html.HtmlSubmitInput;
import com.gargoylesoftware.htmlunit.html.HtmlTable;
import com.gargoylesoftware.htmlunit.html.HtmlTableCell;
import com.gargoylesoftware.htmlunit.html.HtmlTableRow;
import com.gargoylesoftware.htmlunit.html.HtmlTextInput;


public class Main {

	/**
	 * @param args
	 * @throws IOException 
	 * @throws InterruptedException 
	 */
	public static void main(String[] args) throws IOException, InterruptedException {

		
/*****************************************************************************
 * FIRST SEARCH PAGE
 * 
 ****************************************************************************/
	
		
		 final WebClient webClient = new  WebClient(BrowserVersion.FIREFOX_17);
		 webClient.getOptions().setJavaScriptEnabled(true);
		 webClient.getOptions().setCssEnabled(false); // I think this speeds the thing up
		 webClient.getOptions().setRedirectEnabled(true);
		 webClient.setAjaxController(new NicelyResynchronizingAjaxController());
		 webClient.getCookieManager().setCookiesEnabled(true);
		 webClient.setThrowExceptionOnScriptError(true); 
		 webClient.setThrowExceptionOnFailingStatusCode(true); 

		    // Get the first page
		    final HtmlPage searchPage = webClient.getPage("http://www.rpe.rbq.gouv.qc.ca/GIC_Public_NET/RPE/GIC111/GIC111PR01RechercheEntrepreneur.aspx");

		    // Get the form that we are dealing with and within that form, 
		    // find the submit button and the field that we want to change.
		    final HtmlForm form = searchPage.getFormByName("aspnetForm");

		    final HtmlSubmitInput submitButton = form.getInputByName("ctl00$ZonePrincipal$btnRecherche2");
		    final HtmlSelect region = form.getSelectByName("ctl00$ZonePrincipal$ddlRegionAdmin");
		    final HtmlSelect categorie = form.getSelectByName("ctl00$ZonePrincipal$ddlSousCategorie1");

		    /*******************************************************
		     * SELECT THE REGION
		     *******************************************************
		     */
		    // Change the value of the text field
		    // select the value key corresponding to the region to select
		    final HtmlOption regionSelected = region.getOptionByValue("24");
		    regionSelected.setSelected(true);
		    
		    
		    /*******************************************************
		     * SELECT A CATEGORY
		     *******************************************************
		     */
		    
			List<DomNode> supplierCategoryLinks  = (List<DomNode>) searchPage.getByXPath("//select[@id='ctl00_ZonePrincipal_ddlSousCategorie1']//option");
			List<DomAttr> suppliersCategoryCodes = (List<DomAttr>) searchPage.getByXPath("//select[@id='ctl00_ZonePrincipal_ddlSousCategorie1']//@value");


			// i corresponds to the category of supplier
			for (int i=60; i < supplierCategoryLinks.size(); i++){
				System.out.println(supplierCategoryLinks.get(i).getTextContent());
				writeToFile(supplierCategoryLinks.get(i).getTextContent() + "\n");
	
				// Select the current Category
				HtmlOption categorieSelected = categorie.getOptionByValue(suppliersCategoryCodes.get(i).getValue());
			    categorieSelected.setSelected(true);
			    
			    /*
			     *  Submit the form and get the list of category suppliers
			     */
			    
			    // Now submit the form by clicking the button and get back the second page.
			    WebWindow window = searchPage.getEnclosingWindow();
			   HtmlPage  categoryResultsPage = submitButton.click();
			    
			    while(window.getEnclosedPage() == searchPage) {
			        // The page hasn't changed. 
			        Thread.sleep(50);
			    }
			    //webClient.waitForBackgroundJavaScript(5000); 
			    // This loop above will wait until the page changes.
			    categoryResultsPage = (HtmlPage) window.getEnclosedPage();
			    
			    for(int k = 1;; k++){
					 System.out.println("~~~~~~~~~~~~ PAGE : " + k + " ~~~~~~~~~~~~");
				    
					    List<DomAttr> supplierJsLinks = jsFromMainTable(categoryResultsPage);

					
				// if(k>=10){		 
					 
					 for(int j=0; j<supplierJsLinks.size(); j++){
					    	ScriptResult jsResult1 = categoryResultsPage.executeJavaScript(supplierJsLinks.get(j).getValue());
						    HtmlPage supplierPage = (HtmlPage) jsResult1.getNewPage();
						    printSupplier(supplierPage);
						    
						    
						    categoryResultsPage = (HtmlPage) window.getEnclosedPage(); 
						    categoryResultsPage =	(HtmlPage) categoryResultsPage.executeJavaScript("history.back()").getNewPage();
					    }
					 
				//} 
					 //categoryResultsPage = (HtmlPage) window.getEnclosedPage();  
					
					 List<DomNode> nextLinks = (List<DomNode>) categoryResultsPage.getByXPath("//*[@id='A2']");
					 ScriptResult jsResult = categoryResultsPage.executeJavaScript("javascript:modifierParam2('page','"+ Integer.toString(k+1) +"')");
 
					    //webClient.waitForBackgroundJavaScript(500); 

					    categoryResultsPage = (HtmlPage) window.getEnclosedPage(); 
					    //System.out.println(categoryResultsPage.asText());
					    
					    
				    if( nextLinks.isEmpty() ){
				    	System.out.println("No next page");
				    	categoryResultsPage =	(HtmlPage) categoryResultsPage.executeJavaScript("history.back()").getNewPage();
				    	break;
				    }
				    else {
				    	HtmlPage nextPage = (HtmlPage) jsResult.getNewPage();
						 categoryResultsPage = (HtmlPage) window.getEnclosedPage(); 
					     
						// System.out.println(nextPage.asText());
						 
				    	System.out.println("Has Next page");
	
				    }
				    
				    categoryResultsPage = (HtmlPage) window.getEnclosedPage(); 
				    categoryResultsPage =	(HtmlPage) categoryResultsPage.executeJavaScript("history.back()").getNewPage();
				    
				    
				    
			    }
				    
			    
			}
  	    
			System.out.println("**********DOOOONEEEEEEEEEENENENE!!!!!!");
		    webClient.closeAllWindows();
			
	}
	
	
	
    
    /*****************************************************************************
	 * Executing Javascript
	 * 
	 ****************************************************************************/
    
//    ScriptResult jsResult = page2.executeJavaScript("javascript:modifierParam2('1-1AF-59199','8308-9458-18');");
//    webClient.waitForBackgroundJavaScript(5000); 
//    page2 = (HtmlPage) jsResult.getNewPage();
//    textSource = page2.asText();
//    System.out.println(textSource);
    
    
    
    
    //System.out.println(jsFromMainTable(page2).get(8).getValue());
	
	
	
	/*****************************************************************************
	 * HELPER METHODS
	 * 
	 ****************************************************************************/

	public static void printSupplier(HtmlPage supplierPage){
		// gets to the deatils page for each company
	    // Print name
	    String supplierName = ((DomNode)supplierPage.getByXPath("//div[@id='myPrincipal']/table[2]/tbody/tr[2]").get(0)).getTextContent();
	    supplierName = supplierName.replaceAll("\\s"," ");  
	    System.out.print(supplierName.trim() + "\t");
	    writeToFile(supplierName.trim() + "\t");


	    // Print address
	    String address1 = ((DomNode)supplierPage.getByXPath("//div[@id='myPrincipal']/table[2]/tbody/tr[3]//tr[1]").get(0)).getTextContent();
	    String address2 = ((DomNode)supplierPage.getByXPath("//div[@id='myPrincipal']/table[2]/tbody/tr[3]//tr[2]").get(0)).getTextContent();
	    String address3 = ((DomNode)supplierPage.getByXPath("//div[@id='myPrincipal']/table[2]/tbody/tr[3]//tr[3]").get(0)).getTextContent();
	    
	    System.out.print(address1.trim() + "\t");
	    System.out.print(address2.trim()+ "\t");
	    System.out.println(address3.trim());
	    writeToFile(address1.trim() + "\t" + address2.trim() + "\t" + address3.trim() + "\n");

	}
	
	
	public static List<DomAttr> jsFromMainTable(HtmlPage page){
		List<DomAttr> result = (List<DomAttr>) page.getByXPath("//table[@id='ctl00_ZonePrincipal_GridView1']//@href");
		
//		for(int i=0; i<result.size(); i++){
//			System.out.println(result.get(i).getValue());
//		}
		return result;
		
	}
	
	public static  HtmlPage executeJS(WebClient webClient, HtmlPage page, String js) throws InterruptedException{
		//System.out.println(page.asText());
//		System.out.println(js);

		WebWindow window = page.getEnclosingWindow();
		ScriptResult jsResult = page.executeJavaScript(js);
	    webClient.waitForBackgroundJavaScript(500); 
	    HtmlPage resultPage = (HtmlPage) jsResult.getNewPage();
	    resultPage = (HtmlPage) window.getEnclosedPage();
			    
	       
	    //System.out.println(resultPage.asText());
	    return resultPage;
	}
	
	public static void writeToFile(String string){
		try {
			 
 
			File file = new File("C:/Dev/suppliers.txt");
 
			// if file doesnt exists, then create it
			if (!file.exists()) {
				file.createNewFile();
			}
 
			FileWriter fw = new FileWriter(file.getAbsoluteFile(),true);
			BufferedWriter bw = new BufferedWriter(fw);
			bw.write(string);
			bw.close();
 
 
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void writeInputStream(InputStream input) throws IOException{
		
		String result = "";
		
		if (input != null) {
			InputStreamReader inputStreamReader = new InputStreamReader(input);
			BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
			String receiveString = "";
			StringBuilder stringBuilder = new StringBuilder();

			while ((receiveString = bufferedReader.readLine()) != null) {
				stringBuilder.append(receiveString);
			}

			result = stringBuilder.toString();
		}
		
		System.out.println(result);
		
	}
	


}
