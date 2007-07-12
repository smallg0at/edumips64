/* GUICycles.java
 *
 * This class draw the cycles component. It gives a representation of the timing 
 * behaviour of the pipeline.
 * (c) 2006 Filippo Mondello, Trubia Massimo (FPU modifications)
 *
 * This file is part of the EduMIPS64 project, and is released under the GNU
 * General Public License.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package edumips64.ui;


import edumips64.utils.Config;
import edumips64.core.*;
import edumips64.core.is.*;
import java.awt.*;
import javax.swing.*;
import javax.accessibility.*;
import java.awt.event.*;
import java.util.*;

//debug
import edumips64.Main;

/** This class draw the cycles component. It gives a representation of the timing 
* behaviour of the pipeline.
* @author Filippo Mondello
*/
public class GUICycles extends GUIComponent {
		
	Panel1 pannello;
	Panel2 pannello2;
	
	JScrollPane jsp1,jsp2;
	private JSplitPane splitPane;
	int conta,tempo,oldTime,n_instr;
	Instruction [] instr;
	int memoryStalls; // used for understanding if the EX instruction is in structural stall (memory)
	int inputStructuralStalls; // groups three stalls (EXNotAvailable, FuncUnitNotAvailable, DividerNotAvailable) in order to understand if a new instruction has to be added to "lista"
	int RAWStalls, WAWStalls, structStallsEX, structStallsDivider,structStallsFuncUnit;
	boolean flag[]=new boolean[5];
	JButton bottone;


	Map <CPU.PipeStatus,Instruction> pipeline;
	Map<CPU.PipeStatus,Color> colore;
	Dimension dim,dim2;
	
	java.util.List<ElementoCiclo> lista;
	
	public GUICycles(){
		super();
		lista=Collections.synchronizedList(new LinkedList<ElementoCiclo>());
		memoryStalls=cpu.getMemoryStalls();
		inputStructuralStalls=cpu.getStructuralStallsDivider()+cpu.getStructuralStallsEX()+cpu.getStructuralStallsFuncUnit();
		RAWStalls=cpu.getRAWStalls();
		WAWStalls=cpu.getWAWStalls();
		structStallsEX=cpu.getStructuralStallsEX();
		structStallsDivider =cpu.getStructuralStallsDivider();
		structStallsFuncUnit= cpu.getStructuralStallsFuncUnit();
		pannello=new Panel1();
		
		jsp1=new JScrollPane(pannello);//pannello di destra
		dim=new Dimension(20,30);
		pannello.setPreferredSize(dim);
		
		pannello2=new Panel2();
		
		jsp2=new JScrollPane(pannello2);//pannello di sinistra
		dim2=new Dimension(10,30);
		pannello2.setPreferredSize(dim2);
		
		jsp1.setVerticalScrollBar(jsp2.getVerticalScrollBar());

		jsp1.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
		jsp2.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		jsp1.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		
		splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,jsp2,jsp1);
		splitPane.setOneTouchExpandable(true);
		splitPane.setDividerLocation(150);
		
		pipeline=new HashMap<CPU.PipeStatus,Instruction>();
		tempo=0;
		n_instr=0;
		oldTime=0;
		instr=new Instruction[5];
		
		for(int i=0;i<5;i++)
			flag[i]=false;
	}
	
	public void setContainer(Container co){
		super.setContainer(co);
		cont.add(splitPane);
		draw();
	}
	
	
	public synchronized void update(){
		synchronized(pannello) {
			synchronized(pannello2) {
				pipeline=cpu.getPipeline();

				tempo=cpu.getCycles();

				int firstIndex;

				if(oldTime!=tempo){
					if(tempo>0){

						int indice = lista.size();

						instr[0]=pipeline.get(CPU.PipeStatus.IF);
						instr[1]=pipeline.get(CPU.PipeStatus.ID);
						instr[2]=pipeline.get(CPU.PipeStatus.EX);
						instr[3]=pipeline.get(CPU.PipeStatus.MEM);
						instr[4]=pipeline.get(CPU.PipeStatus.WB);

						if( (tempo>4) && (instr[4]!=null) ){

							if(instr[4].getName()!=" "){

								/*
								for(firstIndex=java.lang.Math.min(4,n_instr);firstIndex>0;firstIndex--){
									if(lista.get(indice-firstIndex).getStato().getLast()=="MEM"){
										lista.get(indice-firstIndex).addStato("WB");
										break;
									}
								}
								flag[4]=false;
								*/
								int index= searchListaBySerialNumber(instr[4].getSerialNumber());
								if(index!=-1)
								{
									//if(lista.get(index).getStato().getLast()=="MEM"){
										lista.get(index).addStato("WB");
									//}
								}
								flag[4]=false;
								
							}
							else
								flag[4]=true;
						}

						if((tempo>3)&&(instr[3] != null)){

							if(instr[3].getName()!=" "){
								/*
								for(firstIndex=java.lang.Math.min(3,n_instr);firstIndex>0;firstIndex--){
									if(lista.get(indice-firstIndex).getStato().getLast()=="EX"){
										//we check if the instruction wasn't stopped for a structural stall(memory)
										
										lista.get(indice-firstIndex).addStato("MEM");
										break;
									}

								}
								flag[3]=false;
								 */
								
								int index= searchListaBySerialNumber(instr[3].getSerialNumber());
								if(index!=-1)
								{
									//the instruction has to be tagged as "MEM" in 2 cases. If it holds the "EX" 
									//tag or (in case of "Str") the structural stall (memory) finished 
									//(we compare the previous memoryStalls with the current one)
									String stage;
									stage=lista.get(index).getStato().getLast();
									//if(stage=="EX"  || stage=="StAdd" || stage=="StMul" || stage=="StDiv" 
									//		|| stage=="A4"	  || stage=="M7"    || stage.matches("DIV[0-9][0-9]") ||  memoryStalls==cpu.getMemoryStalls()){
										lista.get(index).addStato("MEM");
									//}
								}
								flag[3]=false;
								
							}
							else
								flag[4]=true;
						}

						if( (tempo>2) && (instr[2]!=null) ){

							if(instr[2].getName()!=" "){
								/*
								for(firstIndex=java.lang.Math.min(2,n_instr);firstIndex>0;firstIndex--){
									if((lista.get(indice-firstIndex).getStato().getLast()=="ID")|| (lista.get(indice-firstIndex).getStato().getLast()=="RAW")){
										lista.get(indice-firstIndex).addStato("EX");
										break;
									}
								}
								flag[2]=false;
								 */
								int index= searchListaBySerialNumber(instr[2].getSerialNumber());
								boolean exTagged=false; //if a structural stall(memory) occurs the instruction in EX has to be tagged with "EX" and succefully with "Str"
								if(index!=-1)
								{
									if(lista.get(index).getStato().getLast()=="ID" || lista.get(index).getStato().getLast()=="RAW" ||  lista.get(index).getStato().getLast()=="WAW" || lista.get(index).getStato().getLast()=="StEx"){
										lista.get(index).addStato("EX");
										exTagged=true;
									}
									//we check if a structural hazard  occurred if there's a difference between the previous value of memoryStall counter and the current one
									if(memoryStalls!=cpu.getMemoryStalls() && !exTagged)


										lista.get(index).addStato("Str");

								}
								flag[2]=false;
								exTagged=false;
								
							}
							else
								flag[2]=true;
						}

						//if there was an inputstructuralstall(including Raw and WAW) we cannot add  a new ElementoCiclo in "lista" and we must add tags as " ", "StDiv", "RAW", "WAW"
						
						//input stalls
						boolean RAWStallOccurred =(RAWStalls!=cpu.getRAWStalls());
						boolean WAWStallOccurred =(WAWStalls!=cpu.getWAWStalls());
						boolean structStallEXOccurred =(structStallsEX!=cpu.getStructuralStallsEX());
						boolean structStallDividerOccured=(structStallsDivider!=cpu.getStructuralStallsDivider());
						boolean structStallsFuncUnitOccurred=(structStallsFuncUnit!=cpu.getStructuralStallsFuncUnit());
						boolean inputStallOccurred=(inputStructuralStalls!=cpu.getStructuralStallsDivider()+cpu.getStructuralStallsEX()+cpu.getStructuralStallsFuncUnit() + cpu.getRAWStalls() + cpu.getWAWStalls());						
						
						if( (tempo>1) && (instr[1]!= null) ){
							/*
							if(instr[1].getName()!=" "){
							
								if(flag[2]==false){
									lista.get(indice-1).addStato("ID");
									flag[1]=false;
								}
								else
									if( (lista.get(indice-2).getStato().getLast()=="ID") || (lista.get(indice-2).getStato().getLast()=="RAW")){
										lista.get(indice-2).addStato("RAW");
										flag[1]=true;
									}
									else if(lista.get(indice-1).getStato().getLast()=="IF"){
										lista.get(indice-1).addStato("ID");
										flag[1]=false;
									}
									else if((lista.get(indice-1).getStato().getLast()=="ID") || (lista.get(indice-1).getStato().getLast()=="RAW")){
										lista.get(indice-1).addStato("RAW");
										flag[1]=true;
									}
							 
								
								
							}
							else
								flag[1]=true;
							 */
							if(instr[1].getName()!=" "){
								int index= searchListaBySerialNumber(instr[1].getSerialNumber());		
								if(!inputStallOccurred)
									if(cpu.getStatus()==CPU.CPUStatus.RUNNING)
										lista.get(index).addStato("ID");
									else
									{
										//the instruction in ID is a bubble because the CPU was stopped from a terminating instruction
									
									}	
								if(RAWStallOccurred)
								{
									if(cpu.getStatus()==CPU.CPUStatus.RUNNING)
										lista.get(index).addStato("RAW");
								}
								if(WAWStallOccurred)
								{
									if(cpu.getStatus()==CPU.CPUStatus.RUNNING)
										lista.get(index).addStato("WAW");
								}
								if(structStallDividerOccured)
								{
									if(cpu.getStatus()==CPU.CPUStatus.RUNNING)
										lista.get(index).addStato("StDiv");
								}
								if(structStallEXOccurred)
								{
									if(cpu.getStatus()==CPU.CPUStatus.RUNNING)
										lista.get(index).addStato("StEx");
								}
								if(structStallsFuncUnitOccurred)
								{
									if(cpu.getStatus()==CPU.CPUStatus.RUNNING)
										lista.get(index).addStato("StFun");
								}
							}	
							
						}

						if(instr[0]!=null){
							/*
							if(flag[1]==false){
								lista.add(new ElementoCiclo(instr[0].getFullName(),tempo,instr[0].getSerialNumber()));
								n_instr++;
							}
							else{
								if(flag[2]==true)
									lista.get(indice-1).addStato(" ");
								else{
									if(!instr[2].getName().toUpperCase().equals("HALT")){
										lista.get(indice-1).setFlagCancella(true);
										lista.add(new ElementoCiclo(instr[0].getFullName(),tempo,instr[0].getSerialNumber()));
										n_instr++;
									}
									else
										lista.get(indice-1).addStato(" ");

								}
							}
							*/
							if(!inputStallOccurred)
							{
								//we must instanciate a new ElementoCiclo only if the CPU is running or there was a JumpException and the the IF instruction was changed
								if(cpu.getStatus()==CPU.CPUStatus.RUNNING)
								{
									lista.add(new ElementoCiclo(instr[0].getFullName(),tempo,instr[0].getSerialNumber()));
									n_instr++;
								}
							}
							else
							{
								int index= searchListaBySerialNumber(instr[0].getSerialNumber());
								if(index!=-1)
								{
									lista.get(index).addStato(" ");
								}
							}
							
						}
						
						//we have to check instructions in the FP pipeline

						//ADDER -------------------------------------------------
						String stage;
						int index;
						Instruction instrSearched;
						if(cpu.getInstructionByFuncUnit("ADDER",1)!=null)
						{

							index= searchListaBySerialNumber(cpu.getInstructionByFuncUnit("ADDER",1).getSerialNumber());
							//boolean A1tagged=false;
							if(index!=-1)
							{
								
								//if(lista.get(index).getStato().getLast()=="ID" || lista.get(index).getStato().getLast()=="A1"  || lista.get(index).getStato().getLast()=="RAW" || lista.get(index).getStato().getLast()=="WAW")
								//{
									lista.get(index).addStato("A1");
									//A1tagged=true;
								//}
								//we check if a structural hazard  occurred and the instruction was stopped 
								//SE SI VUOLE RIPRISTINARE TOGLIERE LA 2^ CONDIZIONE DALL'IF PRECEDENTE'
								//if(!A1tagged && lista.get(index).getStato().getLast()=="A1"/* || lista.get(index).getStato().getLast()=="A-")*/)
								//	lista.get(index).addStato("A1");
							}
							//A1tagged=false;
							
						}						
						
						if(cpu.getInstructionByFuncUnit("ADDER",2)!=null)
						{

							index= searchListaBySerialNumber(cpu.getInstructionByFuncUnit("ADDER",2).getSerialNumber());
							//boolean A2tagged=false;
							if(index!=-1)
							{
								//if(lista.get(index).getStato().getLast()=="A1" || lista.get(index).getStato().getLast()=="A2"/* || lista.get(index).getStato().getLast()=="A-"*/)
								//{
									lista.get(index).addStato("A2");
									//A2tagged=true;
								//}
								//we check if a structural hazard  occurred and the instruction was stopped 
								//if(!A2tagged && lista.get(index).getStato().getLast()=="A2"/* || lista.get(index).getStato().getLast()=="A-")*/)
								//	lista.get(index).addStato("A2");
							}
							//A2tagged=false;
							
						}						
						if(cpu.getInstructionByFuncUnit("ADDER",3)!=null)
						{

							index= searchListaBySerialNumber(cpu.getInstructionByFuncUnit("ADDER",3).getSerialNumber());
							//boolean A3tagged=false;
							if(index!=-1)
							{
								//if(lista.get(index).getStato().getLast()=="A2" || lista.get(index).getStato().getLast()=="A3"/* || lista.get(index).getStato().getLast()=="A-"*/)
								//{
									lista.get(index).addStato("A3");
									//A3tagged=true;
								//}
								//we check if a structural hazard  occurred and the instruction was stopped
								//if(!A3tagged && lista.get(index).getStato().getLast()=="A3"/* || lista.get(index).getStato().getLast()=="A-")*/)
								//	lista.get(index).addStato("A3");
							}
							//A3tagged=false;
							
						}
						
						if(cpu.getInstructionByFuncUnit("ADDER",4)!=null)
						{

							index= searchListaBySerialNumber(cpu.getInstructionByFuncUnit("ADDER",4).getSerialNumber());
							boolean A4tagged=false;
							if(index!=-1)
							{
									
								if(lista.get(index).getStato().getLast()=="A3"/* || lista.get(index).getStato().getLast()=="A-"*/)
								{
									lista.get(index).addStato("A4");
									A4tagged=true;
								}
								//we check if a structural hazard  occurred and involved the divider or the multiplier 
								if(!A4tagged && (lista.get(index).getStato().getLast()=="A4" || lista.get(index).getStato().getLast()=="StAdd"))
									lista.get(index).addStato("StAdd");
							}
							A4tagged=false;
						}
						//MULTIPLIER -----------------------------------------------------
						if((instrSearched=cpu.getInstructionByFuncUnit("MULTIPLIER",1))!=null)
						{

							index= searchListaBySerialNumber(instrSearched.getSerialNumber());
							
							if(index!=-1)
							{
								stage =lista.get(index).getStato().getLast();
								//if(stage=="ID"  || stage=="M1" || stage=="RAW" || stage=="WAW")
									lista.get(index).addStato("M1");
							}
						}
						
						if((instrSearched=cpu.getInstructionByFuncUnit("MULTIPLIER",2))!=null)
						{
							index= searchListaBySerialNumber(instrSearched.getSerialNumber());
							if(index!=-1)
							{
								stage=lista.get(index).getStato().getLast();
								//if(stage=="M1" || stage=="M2")
									lista.get(index).addStato("M2");
							}
						}
						
						if((instrSearched=cpu.getInstructionByFuncUnit("MULTIPLIER",3))!=null)
						{
							index= searchListaBySerialNumber(instrSearched.getSerialNumber());
							if(index!=-1)
							{
								stage=lista.get(index).getStato().getLast();
								//if(stage=="M2" || stage=="M3")
									lista.get(index).addStato("M3");
							}
						}
						
						if((instrSearched=cpu.getInstructionByFuncUnit("MULTIPLIER",4))!=null)
						{
							index= searchListaBySerialNumber(instrSearched.getSerialNumber());
							if(index!=-1)
							{
								stage=lista.get(index).getStato().getLast();
								//if(stage=="M3" || stage=="M4")
									lista.get(index).addStato("M4");
							}
						}
						
						if((instrSearched=cpu.getInstructionByFuncUnit("MULTIPLIER",5))!=null)
						{
							index= searchListaBySerialNumber(instrSearched.getSerialNumber());
							if(index!=-1)
							{
								stage=lista.get(index).getStato().getLast();
								//if(stage=="M4" || stage=="M5")
									lista.get(index).addStato("M5");
							}
						}

						if((instrSearched=cpu.getInstructionByFuncUnit("MULTIPLIER",6))!=null)
						{
							index= searchListaBySerialNumber(instrSearched.getSerialNumber());
							if(index!=-1)
							{
								stage=lista.get(index).getStato().getLast();
								//if(stage=="M5" || stage=="M6")
									lista.get(index).addStato("M6");
							}
						}
								
						if((instrSearched=cpu.getInstructionByFuncUnit("MULTIPLIER",7))!=null)
						{

							index= searchListaBySerialNumber(instrSearched.getSerialNumber());
							boolean M7tagged=false;
	
							if(index!=-1)
							{
									
								if(lista.get(index).getStato().getLast()=="M6")
								{
									lista.get(index).addStato("M7");
									M7tagged=true;
								}
								//we check if a structural hazard  occurred and involved the divider or the multiplier 
								if(!M7tagged && (lista.get(index).getStato().getLast()=="M7" || lista.get(index).getStato().getLast()=="StMul"))
									lista.get(index).addStato("StMul");
							}
							M7tagged=false;
						}
						
						//DIVIDER ------------------------------------------------------
						if((instrSearched=cpu.getInstructionByFuncUnit("DIVIDER",0))!=null)
						{
							boolean DIVtagged=false;
							index= searchListaBySerialNumber(instrSearched.getSerialNumber());
							stage=lista.get(index).getStato().getLast();
							if(index!=-1)
							{
								if(stage!="DIV" && !stage.matches("D[0-2][0-9]"))
								{
									lista.get(index).addStato("DIV");
									DIVtagged=true;
								}
							}
							if(!DIVtagged)
							{
								int divCount=cpu.getDividerCounter();
								String divCountStr=String.valueOf(divCount); //divCount in the format DXX (XX belongs to [00  24])
								lista.get(index).addStato((divCount<10) ? "D0"+divCountStr : "D"+ divCountStr);
							}
							DIVtagged=false;
								
						}
						

						
					}
					else{
						lista.clear();
						oldTime=0;
						n_instr=0;
						for(int i=0;i<5;i++)
							flag[i]=false;
					}
					oldTime=tempo;
					
				}
				memoryStalls=cpu.getMemoryStalls();
				inputStructuralStalls=cpu.getStructuralStallsDivider()+cpu.getStructuralStallsEX()+cpu.getStructuralStallsFuncUnit()+ cpu.getRAWStalls() + cpu.getWAWStalls();
				RAWStalls=cpu.getRAWStalls();
				WAWStalls=cpu.getWAWStalls();
				structStallsEX=cpu.getStructuralStallsEX();
				structStallsDivider =cpu.getStructuralStallsDivider();
				structStallsFuncUnit= cpu.getStructuralStallsFuncUnit();
			}
		}
	}
	
	
	public int searchListaBySerialNumber(long serialNumber)
	{
		ElementoCiclo ec;
		for(ListIterator it=lista.listIterator(lista.size());it.hasPrevious();)
		{
			ec=(ElementoCiclo)it.previous();
System.out.println("\nindice " + (it.previousIndex()+1));				
			if(ec.getSerialNumber()==serialNumber)
				return it.previousIndex()+1;
		}
		return -1;
	}
	
	public synchronized void draw(){

		dim.setSize(20 + tempo*30,30+n_instr*15);
		if(30+n_instr*15>pannello2.getHeight())
		dim2.setSize(splitPane.getDividerLocation(),30+n_instr*15);
		else
		dim2.setSize(splitPane.getDividerLocation(),pannello2.getHeight());
		jsp1.getViewport().setViewSize(dim);
		jsp2.getViewport().setViewSize(dim2);
		jsp2.getViewport().setViewPosition(new Point(0,n_instr*15));
		jsp1.getViewport().setViewPosition(new Point(tempo*30,n_instr*15));
		
		cont.repaint();
	}
	
	
	class Panel1 extends JPanel{
	
		public synchronized void paintComponent(Graphics g){
			super.paintComponent(g); // va fatto sempre
			setBackground(Color.white); // fondo bianco
			
			g.setColor(Color.black);

			Font f1 = new Font("Arial", Font.PLAIN, 11); 
			FontMetrics fm1 = g.getFontMetrics(f1); 
			g.setFont(f1); 
			
			riempi(g);
			
		}	
		
		
		public synchronized void riempi(Graphics g){
			int i=0;
			for(ElementoCiclo el:lista){
				int j=0;
				String pre="IF";
				String ext_st="";
				int tempo=el.getTime();
				for(String st:el.getStato()){

					ext_st="";
					if(st.equals("IF"))
						g.setColor((Color)Config.get("IFColor"));
					else if(st.equals("ID"))
						g.setColor((Color)Config.get("IDColor"));
					else if(st.equals("EX"))
						g.setColor((Color)Config.get("EXColor"));
					else if(st.equals("MEM"))
						g.setColor((Color)Config.get("MEMColor"));
					else if(st.equals("WB"))
						g.setColor((Color)Config.get("WBColor"));
					else if(st.equals("Str"))
						g.setColor((Color)Config.get("EXColor"));
					else if(st.equals("A1") || st.equals("A2") || st.equals("A3") || st.equals("A4") || st.equals("StAdd"))
						g.setColor((Color)Config.get("FPAdderColor"));
					else if(st.equals("M1") || st.equals("M2")|| st.equals("M3") || st.equals("M4") || st.equals("M5") || st.equals("M6") || st.equals("M7") || st.equals("StMul"))
						g.setColor((Color)Config.get("FPMultiplierColor"));
					else if(st.matches("D[0-2][0-9]") || st.matches("DIV"))
						g.setColor((Color)Config.get("FPDividerColor"));
					else if(st.equals("RAW")){
						//if(pre.equals("ID")){
						//	ext_st="RAW";
							g.setColor((Color)Config.get("IDColor"));
						//}
					}
					else if(st.equals("WAW") || st.equals("StDiv") || st.equals("StEx") || st.equals("StFun")){
						g.setColor((Color)Config.get("IDColor"));	
					}
					else if(st.equals(" ")){
						if(pre.equals("IF")){
							ext_st=" ";
							g.setColor((Color)Config.get("IFColor"));
						}
					}
					g.fillRect(10+(tempo+j-1)*30,9+i*15,30,13);
					g.setColor(Color.black);
					g.drawRect(10+(tempo+j-1)*30,9+i*15,30,13);
					g.drawString(st, 15+(tempo+j-1)*30,20+i*15);
					j++;
					if((!st.equals(" "))&&(!st.equals("RAW")))
						pre=st;
				}/*
				if((ext_st==" ")&&(el.getFlagCancella()==false))
					g.setColor((Color)Config.get("SAMEIFColor"));
				else if(ext_st=="RAW")
					g.setColor((Color)Config.get("RAWColor"));
				else
					g.setColor(Color.black);*/
				i++;
			}
		}
	}
	
	
	class Panel2 extends JPanel{
	
		public synchronized void paintComponent(Graphics g){
			super.paintComponent(g); // va fatto sempre
			setBackground(Color.white); // fondo bianco
			
			g.setColor(Color.black);

			
			Font f1 = new Font("Arial", Font.PLAIN, 11); 
			FontMetrics fm1 = g.getFontMetrics(f1); 
			g.setFont(f1); 
			
			int i=0;
			for(ElementoCiclo el:lista){
				g.drawString(el.getName(), 5,20+i*15);
				i++;
			}
		}
	}
	
}
