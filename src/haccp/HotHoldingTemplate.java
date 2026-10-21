package haccp;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.zip.*;
import javax.xml.XMLConstants;
import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.*;

/** Edits only cell values in the supplied original workbook; retains all other ZIP entries. */
public final class HotHoldingTemplate {
 private static final String NS="http://schemas.openxmlformats.org/spreadsheetml/2006/main";
 private HotHoldingTemplate(){}
 public static byte[] generate(Path template, YearMonth month, String establishment, String equipment, List<Map<String,String>> entries)throws Exception {
  if(entries.size()>31)throw new IllegalArgumentException("O modelo original só tem 31 linhas disponíveis");
  if(!Files.isRegularFile(template))throw new FileNotFoundException("Modelo em falta: "+template);
  ByteArrayOutputStream result=new ByteArrayOutputStream();
  try(ZipFile input=new ZipFile(template.toFile()); ZipOutputStream output=new ZipOutputStream(result)){
   Enumeration<? extends ZipEntry> all=input.entries();boolean found=false;
   while(all.hasMoreElements()){
    ZipEntry old=all.nextElement();ZipEntry copy=new ZipEntry(old.getName());copy.setTime(old.getTime());output.putNextEntry(copy);
    byte[] bytes;try(InputStream in=input.getInputStream(old)){bytes=in.readAllBytes();}
    if(old.getName().equals("xl/worksheets/sheet1.xml")){
     found=true;
     DocumentBuilderFactory f=DocumentBuilderFactory.newInstance();f.setNamespaceAware(true);
     f.setFeature("http://apache.org/xml/features/disallow-doctype-decl",true);
     f.setFeature("http://xml.org/sax/features/external-general-entities",false);
     f.setFeature("http://xml.org/sax/features/external-parameter-entities",false);
     f.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING,true);
     Document d=f.newDocumentBuilder().parse(new ByteArrayInputStream(bytes));
     put(d,"B2",month.format(DateTimeFormatter.ofPattern("MMMM yyyy",new Locale("pt","PT"))),false);
     put(d,"B3",establishment,false);put(d,"B4",equipment,false);
     // Multiple readings may use different minima; no false single minimum is written.
     String minimum=entries.isEmpty()?"":entries.get(0).getOrDefault("minimum_c","");
     boolean same=entries.stream().allMatch(e->Objects.equals(e.getOrDefault("minimum_c",""),minimum));
     put(d,"B5",same?minimum:"Variável (por registo)",false);
     for(int i=0;i<entries.size();i++){
      Map<String,String> e=entries.get(i);int n=i+8;
      String measured=e.getOrDefault("measured_at","");
      put(d,"A"+n,measured.length()>=10?measured.substring(0,10):"",false);
      put(d,"B"+n,e.getOrDefault("food",""),false);
      put(d,"C"+n,measured.length()>=16?measured.substring(11,16):"",false);
      put(d,"D"+n,e.getOrDefault("temperature_c",""),true);
      put(d,"E"+n,"ok".equals(e.get("status"))?"Sim":"Não",false);
      put(d,"F"+n,e.getOrDefault("corrective_action",""),false);
      put(d,"G"+n,e.getOrDefault("initials",""),false);
     }
     TransformerFactory tf=TransformerFactory.newInstance();tf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING,true);
     Transformer tr=tf.newTransformer();tr.setOutputProperty(OutputKeys.ENCODING,"UTF-8");
     ByteArrayOutputStream b=new ByteArrayOutputStream();tr.transform(new DOMSource(d),new StreamResult(b));bytes=b.toByteArray();
    }
    output.write(bytes);output.closeEntry();
   }
   if(!found)throw new IOException("O modelo não contém xl/worksheets/sheet1.xml");
  }
  return result.toByteArray();
 }
 private static void put(Document d,String address,String value,boolean numeric){
  NodeList cells=d.getElementsByTagNameNS(NS,"c");Element cell=null;
  for(int i=0;i<cells.getLength();i++){Element c=(Element)cells.item(i);if(address.equals(c.getAttribute("r"))){cell=c;break;}}
  if(cell==null)throw new IllegalStateException("Célula não encontrada: "+address);
  while(cell.hasChildNodes())cell.removeChild(cell.getFirstChild());
  if(numeric&&value.matches("-?[0-9]+([.,][0-9]+)?")){
   cell.removeAttribute("t");Element v=d.createElementNS(NS,"v");v.setTextContent(value.replace(',','.'));cell.appendChild(v);
  }else{
   cell.setAttribute("t","inlineStr");Element is=d.createElementNS(NS,"is");Element t=d.createElementNS(NS,"t");t.setTextContent(value);is.appendChild(t);cell.appendChild(is);
  }
 }
}
