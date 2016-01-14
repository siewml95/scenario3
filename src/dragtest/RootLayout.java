package dragtest;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;

import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.SplitPane;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.scene.input.DragEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;

public class RootLayout extends AnchorPane{

	@FXML SplitPane base_pane;
	@FXML AnchorPane right_pane;
	@FXML VBox left_pane;
        @FXML ChoiceBox choice;

	private DragIcon mDragOverIcon = null;
	
	private EventHandler<DragEvent> mIconDragOverRoot = null;
	private EventHandler<DragEvent> mIconDragDropped = null;
	private EventHandler<DragEvent> mIconDragOverRightPane = null;
        private static Connection con;
        private static Statement stat;
        private PreparedStatement prep;
        private int projectid = -1;
        private ObservableList<Integer> data;
	
	public RootLayout() {
		
		FXMLLoader fxmlLoader = new FXMLLoader(
				getClass().getResource("RootLayout.fxml")
				);
		
		fxmlLoader.setRoot(this); 
		fxmlLoader.setController(this);
		
		try { 
			fxmlLoader.load();
        
		} catch (IOException exception) {
		    throw new RuntimeException(exception);
		}
	}
	
	@FXML
	private void initialize() {
		
		//Add one icon that will be used for the drag-drop process
		//This is added as a child to the root anchorpane so it can be visible
		//on both sides of the split pane.
		mDragOverIcon = new DragIcon();
		
		mDragOverIcon.setVisible(false);
		mDragOverIcon.setOpacity(0.65);
		getChildren().add(mDragOverIcon);
		
		//populate left pane with multiple colored icons for testing
		for (int i = 0; i < 8; i++) {
			
			DragIcon icn = new DragIcon();
			
			addDragDetection(icn);
			
			icn.setType(DragIconType.values()[i]);
			left_pane.getChildren().add(icn);
		}
		
		buildDragHandlers();
                  try {
                 con = DataBaseConnection.getConnected();
                 stat = con.createStatement();
                //  stat.executeUpdate("drop table if exists node");
               //   stat.executeUpdate("drop table if exists projectid");
               //   stat.executeUpdate("drop table if exists edge");


                 stat.executeUpdate("create table if not exists node(id varchar(100) ,type integer,x double,y double, projectid integer);");
                 stat.executeUpdate("create table if not exists edge(sourceId varchar(100), targetId varchar(100), projectid integer);");
                 stat.executeUpdate("create table if not exists projectid(id integer primary key autoincrement);");
                  data = FXCollections.observableArrayList();
                 ResultSet rs = con.createStatement().executeQuery("select * from projectid");                 
                 while (rs.next()) {
                    data.add(rs.getInt("id"));
                 }
                 choice.setItems(data);
                
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Error on Building Data");
        }     
        }

        
         @FXML
           private void saveNodes(ActionEvent event) throws SQLException {    
              if(projectid == -1){
                   stat.executeUpdate("insert into projectid values($next_id);");  
                   ResultSet result = stat.executeQuery("select max(id) from projectid;");
                   projectid = result.getInt(1);
                   
                   ResultSet rs = con.createStatement().executeQuery("select * from projectid");
                   ObservableList<Integer> data2 = FXCollections.observableArrayList();
                   while (rs.next()) {
                      data2.add(rs.getInt("id"));
                    }
                   choice.setItems(data2);
                   choice.getSelectionModel().select((Integer) projectid );
                   
              }
             System.out.println("projectid " + projectid);
              prep = con.prepareStatement("delete from node where projectid=" + projectid + ";");
              prep.execute();
              prep = con.prepareStatement("delete from edge where projectid=" + projectid + ";");

              prep.execute();
              ResultSet result = stat.executeQuery("select count(*) from node where projectid=" + projectid + ";");
              System.out.println("rows "+ result.getInt(1));
              for (Node node : right_pane.getChildrenUnmodifiable()) {                
                if (node instanceof DraggableNode) {
                    try {
                    
                   
                    prep = con.prepareStatement("insert into node values(?,?,?,?,?);");
                     prep.setString(1, (((DraggableNode) node).getId()));
                    prep.setInt(2,dragIconTypeToInt(((DraggableNode) node).getType()));
                    prep.setDouble(3, (((DraggableNode) node).getLayoutX ()));
                    prep.setDouble(4,(((DraggableNode) node).getLayoutY()));
                    prep.setInt(5, projectid);
                    prep.execute();
                      System.out.println("Node X : " + ((DraggableNode) node).getLayoutX () );

                    
                    }
                  catch (SQLException ex) {
                     
                  }  
                }
                 else {
                    if (node instanceof NodeLink) {
                        try {
                            prep = con.prepareStatement("insert into edge values(?,?,?);");
                            prep.setString(1, ((NodeLink) node).getSourceNodeId());
                            prep.setString(2, ((NodeLink) node).getTargetNodeId());
                            prep.setInt(3, projectid);
                             prep.execute();
                        } catch (SQLException ex) {
                     
                  }  
                    }
                            
                    }
                            
              }
              

           }
           
           @FXML
           private void newProject(ActionEvent event) {
                right_pane.getChildren().clear();
                choice.getSelectionModel().select(null);
                projectid = -1;
           }
           
           @FXML
           private void loadProject(ActionEvent event) throws SQLException{
              right_pane.getChildren().clear();

               int project = (int) choice.getSelectionModel().getSelectedItem();
               
               System.out.println("Int" + project);
                try {
                    ResultSet rs = con.createStatement().executeQuery("select * from node where projectid=" + project +";");
                
                     while (rs.next()) {
                     System.out.println("X : " + rs.getDouble("x"));
                           DraggableNode droppedIcon = new DraggableNode(rs.getString("id"));
                           droppedIcon.setType(intToDragIconType(rs.getInt("type")));
                           right_pane.getChildren().add(droppedIcon);
                           double x = rs.getDouble("x") + 107;
                           double y = rs.getDouble("y") - 1;
                           droppedIcon.relocateToPoint(new Point2D(x,y)); 
                 }
                  rs = con.createStatement().executeQuery("select * from edge where projectid=" + project +";");
                      while (rs.next()) {
                         NodeLink link = new NodeLink();
                         right_pane.getChildren().add(0,link);
						
						DraggableNode source = null;
						DraggableNode target = null;
						
						for (Node n: right_pane.getChildren()) {
							
							if (n.getId() == null)
								continue;
							
							if (n.getId().equals(rs.getString("sourceId"))) {
								source = (DraggableNode) n;
                                                                link.setSourceNodeId(rs.getString("sourceId"));
                                                        }
						
							if (n.getId().equals(rs.getString("targetId"))) {
								target = (DraggableNode) n;
                                                                link.setTargetNodeId(rs.getString("targetId"));
                                                        }
							
						}
						
						if (source != null && target != null)
							link.bindEnds2(source, target);
                 }
                  
                     projectid=project;
           } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Error on Building Data");
           }
           }
           
          @FXML
           private void deleteProject(ActionEvent event) throws SQLException{              
               int project;
               try {
                  project = (int) choice.getSelectionModel().getSelectedItem();
               } catch(Exception e)
               {
                   project = -1000;
               }
   
               if(projectid == -1  && project == -1000 ) {
                   right_pane.getChildren().clear();
                   System.out.println("Deleted unsaved project");
               }
               else if(project == projectid) {
                   project = projectid;
                   right_pane.getChildren().clear();
                   prep = con.prepareStatement("delete from node where projectid=" + projectid + ";");
                   prep.execute();
                   prep = con.prepareStatement("delete from edge where projectid=" + projectid + ";");
                   prep.execute();
                   prep = con.prepareStatement("delete from projectid where id=" + projectid + ";");
                   prep.execute();
                   projectid=-1;
                   ResultSet rs = con.createStatement().executeQuery("select * from projectid");  
                   ObservableList<Integer> data2 = FXCollections.observableArrayList();
                   while (rs.next()) {
                      data2.add(rs.getInt("id"));
                    }
                   choice.setItems(data2);
                   choice.getSelectionModel().select(null);
                   System.out.println("Deleted Int : " + project);
                   
               }
               else {
                    project = (int) choice.getSelectionModel().getSelectedItem();
                   prep = con.prepareStatement("delete from node where projectid=" + project + ";");
                   prep.execute();
                   prep = con.prepareStatement("delete from edge where projectid=" + project + ";");
                   prep.execute();
                   prep = con.prepareStatement("delete from projectid where id=" + project + ";");
                   prep.execute();                   
                   ResultSet rs = con.createStatement().executeQuery("select * from projectid");  
                   ObservableList<Integer> data2 = FXCollections.observableArrayList();
                   while (rs.next()) {
                      data2.add(rs.getInt("id"));
                    }
                   choice.setItems(data2);
                   choice.getSelectionModel().select((Integer) projectid );              
                   System.out.println("Deleted Int : " + project);
               }
               
               
                
           }
               


 private DragIconType intToDragIconType(int type){
         if(type == 0){
             return DragIconType.valueOf("svitch");
         }
         else if(type == 1) {
             return DragIconType.valueOf("led");
         }
         else if(type == 2) {
              return DragIconType.valueOf("resistor");
         }
         else if(type == 3) {
             return DragIconType.valueOf("cell");
         }
         else if(type == 4){
             return DragIconType.valueOf("relay");
         }
         else if(type == 5){
             return DragIconType.valueOf("yellow");
         }
         else {
             return DragIconType.valueOf("black");
         }
     }
     
     private int dragIconTypeToInt(DragIconType type){
         if(type.equals(DragIconType.svitch)){
            return 0;
         }
         else if(type.equals(DragIconType.led)){
            return 1;
         }
         else if(type.equals(DragIconType.resistor)){
            return 2;
         }
         else if(type.equals(DragIconType.cell)){
            return 3;
         }
         else if(type.equals(DragIconType.relay)){
            return 4;
         }
         else if(type.equals(DragIconType.yellow)){
             return 5;
         }         
         else {
             return 6;
         }
     
	}
	
	private void addDragDetection(DragIcon dragIcon) {
		
		dragIcon.setOnDragDetected (new EventHandler <MouseEvent> () {

			@Override
			public void handle(MouseEvent event) {

				// set drag event handlers on their respective objects
				base_pane.setOnDragOver(mIconDragOverRoot);
				right_pane.setOnDragOver(mIconDragOverRightPane);
				right_pane.setOnDragDropped(mIconDragDropped);
				
				// get a reference to the clicked DragIcon object
				DragIcon icn = (DragIcon) event.getSource();
				
				//begin drag ops
				mDragOverIcon.setType(icn.getType());
				mDragOverIcon.relocateToPoint(new Point2D (event.getSceneX(), event.getSceneY()));
            
				ClipboardContent content = new ClipboardContent();
				DragContainer container = new DragContainer();
				
				container.addData ("type", mDragOverIcon.getType().toString());
				content.put(DragContainer.AddNode, container);

				mDragOverIcon.startDragAndDrop (TransferMode.ANY).setContent(content);
				mDragOverIcon.setVisible(true);
				mDragOverIcon.setMouseTransparent(true);
				event.consume();					
			}
		});
	}	
	
	private void buildDragHandlers() {
		
		//drag over transition to move widget form left pane to right pane
		mIconDragOverRoot = new EventHandler <DragEvent>() {

			@Override
			public void handle(DragEvent event) {
				
				Point2D p = right_pane.sceneToLocal(event.getSceneX(), event.getSceneY());

				//turn on transfer mode and track in the right-pane's context 
				//if (and only if) the mouse cursor falls within the right pane's bounds.
				if (!right_pane.boundsInLocalProperty().get().contains(p)) {
					
					event.acceptTransferModes(TransferMode.ANY);
					mDragOverIcon.relocateToPoint(new Point2D(event.getSceneX(), event.getSceneY()));
					return;
				}

				event.consume();
			}
		};
		
		mIconDragOverRightPane = new EventHandler <DragEvent> () {

			@Override
			public void handle(DragEvent event) {

				event.acceptTransferModes(TransferMode.ANY);
				
				//convert the mouse coordinates to scene coordinates,
				//then convert back to coordinates that are relative to 
				//the parent of mDragIcon.  Since mDragIcon is a child of the root
				//pane, coodinates must be in the root pane's coordinate system to work
				//properly.
				mDragOverIcon.relocateToPoint(
								new Point2D(event.getSceneX(), event.getSceneY())
				);
				event.consume();
			}
		};
				
		mIconDragDropped = new EventHandler <DragEvent> () {

			@Override
			public void handle(DragEvent event) {
				
				DragContainer container = 
						(DragContainer) event.getDragboard().getContent(DragContainer.AddNode);
				
				container.addData("scene_coords", 
						new Point2D(event.getSceneX(), event.getSceneY()));
				
				ClipboardContent content = new ClipboardContent();
				content.put(DragContainer.AddNode, container);
				
				event.getDragboard().setContent(content);
				event.setDropCompleted(true);
			}
		};
		
		this.setOnDragDone (new EventHandler <DragEvent> (){
			
			@Override
			public void handle (DragEvent event) {
				
				right_pane.removeEventHandler(DragEvent.DRAG_OVER, mIconDragOverRightPane);
				right_pane.removeEventHandler(DragEvent.DRAG_DROPPED, mIconDragDropped);
				base_pane.removeEventHandler(DragEvent.DRAG_OVER, mIconDragOverRoot);
								
				mDragOverIcon.setVisible(false);
				
				//Create node drag operation
				DragContainer container = 
						(DragContainer) event.getDragboard().getContent(DragContainer.AddNode);
				
				if (container != null) {
					if (container.getValue("scene_coords") != null) {
					
						if (container.getValue("type").equals(DragIconType.cubic_curve.toString())) {
							CubicCurveDemo curve = new CubicCurveDemo();
							
							right_pane.getChildren().add(curve);
							
							Point2D cursorPoint = container.getValue("scene_coords");

							curve.relocateToPoint(
									new Point2D(cursorPoint.getX() - 32, cursorPoint.getY() - 32)
									);							
						}
						else {
							
							DraggableNode node = new DraggableNode();
							
							node.setType(DragIconType.valueOf(container.getValue("type")));
							right_pane.getChildren().add(node);
	
							Point2D cursorPoint = container.getValue("scene_coords");
	
							node.relocateToPoint(
									new Point2D(cursorPoint.getX() - 32, cursorPoint.getY() - 32)
									);
						}
					}
				}
				/*
				//Move node drag operation
				container = 
						(DragContainer) event.getDragboard().getContent(DragContainer.DragNode);
				
				if (container != null) {
					if (container.getValue("type") != null)
						System.out.println ("Moved node " + container.getValue("type"));
				}
				*/

				//AddLink drag operation
				container =
						(DragContainer) event.getDragboard().getContent(DragContainer.AddLink);
				
				if (container != null) {
					
					//bind the ends of our link to the nodes whose id's are stored in the drag container
					String sourceId = container.getValue("source");
					String targetId = container.getValue("target");

					if (sourceId != null && targetId != null) {
						
						//	System.out.println(container.getData());
						NodeLink link = new NodeLink();
						
						//add our link at the top of the rendering order so it's rendered first
						right_pane.getChildren().add(0,link);
						
						DraggableNode source = null;
						DraggableNode target = null;
						
						for (Node n: right_pane.getChildren()) {
							
							if (n.getId() == null)
								continue;
							
							if (n.getId().equals(sourceId)) {
								source = (DraggableNode) n;
                                                                link.setSourceNodeId(sourceId);
                                                        }
						
							if (n.getId().equals(targetId)) {
								target = (DraggableNode) n;
                                                                link.setTargetNodeId(targetId);
                                                        }
							
						}
						
						if (source != null && target != null)
							link.bindEnds(source, target);
					}
						
				}

				event.consume();
			}
		});		
	}
}
