package org.tqdev.visarray;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;
import org.tqdev.visarray.model.Cloud;
import org.tqdev.visarray.model.Mesh;
import org.tqdev.visarray.model.Model;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import de.schlichtherle.truezip.file.TFile;
import de.schlichtherle.truezip.file.TFileInputStream;

/**
 *
 * @author James Sweet
 */
public class XMLParse {
    List<Model> Models = new ArrayList<Model>();
    
    public void Parse( TFile stream ) throws Exception {
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse( new TFileInputStream( stream ) );
        doc.getDocumentElement().normalize();

        // Data
        ParseRoot( doc.getDocumentElement(), stream.getParent() );
        
        System.out.println("Parsed: " + stream.getName() );
        System.out.println("\tFound " + Models.size() + " models" );
    }
    
    private void ParseRoot( Node root, String path ){
        // Models
        NodeList nodes = root.getChildNodes();
        for(int i=0; i<nodes.getLength(); i++){
            Node node = nodes.item(i);

            if(node instanceof Element){
                ParseModel( node, path );
            }
        }
    }
    
    private void ParseModel( Node model, String path ){
        Model current = new Model();
        
        NodeList nodes = model.getChildNodes();
        for(int i=0; i< nodes.getLength(); i++){
            Node node = nodes.item(i);
            
            if(node instanceof Element){
                Element child = (Element) node;
                
                if( "title".equals(child.getTagName()) ){
                    current.Title = child.getTextContent();
                }
                
                if( "packagepath".equals(child.getTagName()) ){
                    current.Path = path + "/" + child.getTextContent();
                }
                
                if( "radius".equals(child.getTagName()) ){
                    current.Radius = Float.parseFloat( child.getTextContent() );
                }
                
                if( "view".equals(child.getTagName()) ){
                    String[] part = child.getTextContent().trim().split( ",\\s+", 4 );
                    
                    final float x = Float.parseFloat( part[0] );
                    final float y = Float.parseFloat( part[1] );
                    final float z = Float.parseFloat( part[2] );
                    final float w = Float.parseFloat( part[3] );
                    
                    current.View = new Vector4f( x, y, z, w );
                }
                
                if( "center".equals(child.getTagName()) ){
                    String[] part = child.getTextContent().trim().split( ",\\s+", 3 );
                    
                    final float x = Float.parseFloat( part[0] );
                    final float y = Float.parseFloat( part[1] );
                    final float z = Float.parseFloat( part[2] );
                    
                    current.Center = new Vector3f( x, y, z );
                }
                
                if( "container".equals(child.getTagName()) ){
                    ParseContainer( node, current );
                }
                
                if( "mesh".equals(child.getTagName()) ){
                    Mesh mesh = ParseMesh( node, current.Path );
                    current.Meshes.add(mesh);
                }
                
                if( "cloud".equals(child.getTagName()) ){
                    Cloud cloud = ParseCloud( node, current.Path );
                    current.Clouds.add(cloud);
                }
            }
        }
        
        Models.add( current );
    }
    
    private void ParseContainer( Node container, Model model ){
        FloatBuffer Transform = ByteBuffer.allocateDirect(16*4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        
        NodeList nodes = container.getChildNodes();
        
        // Get Transform First
        for(int i=0; i< nodes.getLength(); i++){
            Node node = nodes.item(i);
            
            if(node instanceof Element){
                Element child = (Element) node;
                
                if( "transformmatrix".equals(child.getTagName()) ){
                    String[] part = child.getTextContent().trim().split( ",\\s+", 16 );
                    
                    for( int j = 0; j < 16; j++ ){
                        Transform.put( Float.parseFloat( part[j] ) );
                    }
                    Transform.rewind();
                }
            }
        }
        
        
        // Load Meshs and Clouds
        for(int i=0; i< nodes.getLength(); i++){
            Node node = nodes.item(i);
            
            if(node instanceof Element){
                Element child = (Element) node;
                
                if( "mesh".equals(child.getTagName()) ){
                    Mesh mesh = ParseMesh( node, model.Path );
                    mesh.Transformation = Transform.asReadOnlyBuffer();
                    mesh.Transformation.rewind();
                    model.Meshes.add(mesh);
                }
                
                if( "cloud".equals(child.getTagName()) ){
                    Cloud cloud = ParseCloud( node, model.Path );
                    cloud.Transformation = Transform.asReadOnlyBuffer();
                    cloud.Transformation.rewind();
                    model.Clouds.add(cloud);
                }
            }
        }
    }
    
    private Mesh ParseMesh( Node mesh, String path ){
        Mesh retVal = new Mesh();
        
        NodeList nodes = mesh.getChildNodes();
        for(int i=0; i< nodes.getLength(); i++){
            Node node = nodes.item(i);
            
            if(node instanceof Element){
                Element child = (Element) node;
                
                if( "transformmatrix".equals(child.getTagName()) ){
                    String[] part = child.getTextContent().trim().split( ",\\s+", 16 );
                    
                    for( int j = 0; j < 16; j++ ){
                        retVal.Transformation.put( Float.parseFloat( part[j] ) );
                    }
                    retVal.Transformation.rewind();
                }
                
                if( "numberpoints".equals(child.getTagName()) ){
                    retVal.Verticies = Integer.parseInt( child.getTextContent() );
                }
                
                if( "numberindexes".equals(child.getTagName()) ){
                    retVal.Indicies = Integer.parseInt( child.getTextContent() );
                }
                
                if( "texture".equals(child.getTagName()) ){
                    retVal.Texture = path + "/" + child.getTextContent();
                }
                
                if( "points".equals(child.getTagName()) ){
                    retVal.SetVertex( path + "/" + child.getTextContent() );
                }
                
                if( "indexes".equals(child.getTagName()) ){
                    retVal.SetIndex( path + "/" + child.getTextContent() );
                }
            }
        }
        
        return retVal;
    }
    
    private Cloud ParseCloud( Node cloud, String path ){
        Cloud retVal = new Cloud();
        
        NodeList nodes = cloud.getChildNodes();
        for(int i=0; i< nodes.getLength(); i++){
            Node node = nodes.item(i);
            
            if(node instanceof Element){
                Element child = (Element) node;
                
                if( "transformmatrix".equals(child.getTagName()) ){
                    String[] part = child.getTextContent().trim().split( ",\\s+", 16 );
                    
                    for( int j = 0; j < 16; j++ ){
                        retVal.Transformation.put( Float.parseFloat( part[j] ) );
                    }
                    retVal.Transformation.rewind();
                }
                
                if( "pointscale".equals(child.getTagName()) ){
                    retVal.PointScale = Float.parseFloat( child.getTextContent() );
                }
                
                if( "numbercolorpoints".equals(child.getTagName()) ){
                    retVal.Verticies = Integer.parseInt( child.getTextContent() );
                }
                
                if( "colorpoints".equals(child.getTagName()) ){
                    retVal.SetVertex( path + "/" + child.getTextContent() );
                }
            }
        }
        
        return retVal;
    }
}
