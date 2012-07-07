package org.tqdev.visarray;

import de.schlichtherle.truezip.file.TFile;
import de.schlichtherle.truezip.file.TFileInputStream;
import java.io.DataInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.lwjgl.BufferUtils;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import org.newdawn.slick.opengl.Texture;
import org.newdawn.slick.opengl.TextureLoader;
import org.tqdev.visarray.opengl.VertexBufferObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 * @author James Sweet
 */
public class XMLParse {
    class Mesh{
        FloatBuffer Transformation = BufferUtils.createFloatBuffer(16);
        
        int Verticies, Indicies;
        String Texture = "", Vertex, Index;
        
        private Texture TextureFile = null;
        
        private ByteBuffer DataBuffer = null;
        private VertexBufferObject DataVBO = null;
        
        private ByteBuffer IndexBuffer = null;
        private VertexBufferObject IndexVBO = null;
        
        public void SetVertex( String Path ){
            Vertex = Path;
            
            try{
                TFile vFile = new TFile(Vertex);
                DataInputStream vIn = new DataInputStream(new TFileInputStream(vFile));
                byte[] vInput = new byte[(int)vFile.length()];
                vIn.readFully(vInput);
                DataBuffer = BufferUtils.createByteBuffer((int)vFile.length());
                DataBuffer.put(vInput);
                DataBuffer.rewind();
                vIn.close();
            }catch( Exception e ){

            }
        }
        
        public void SetIndex( String Path ){
            Index = Path;
            
            try{
                TFile vFile = new TFile(Index);
                DataInputStream vIn = new DataInputStream(new TFileInputStream(vFile));
                byte[] vInput = new byte[(int)vFile.length()];
                vIn.readFully(vInput);
                IndexBuffer = BufferUtils.createByteBuffer((int)vFile.length()).order(ByteOrder.nativeOrder());
                IndexBuffer.put(vInput);
                IndexBuffer.rewind();
                vIn.close();
            }catch( Exception e ){

            }
        }
        
        public void Draw(){
            if( DataVBO == null && DataBuffer != null ){
                DataVBO = new VertexBufferObject();
                DataVBO.Create();
                DataVBO.Bind(GL_ARRAY_BUFFER);
                {
                    DataVBO.Upload(GL_ARRAY_BUFFER, DataBuffer);
                }
                DataVBO.Unbind(GL_ARRAY_BUFFER);   
            }
            
            if( IndexVBO == null && IndexBuffer != null){
                IndexVBO = new VertexBufferObject();
                IndexVBO.Create();
                IndexVBO.Bind(GL_ELEMENT_ARRAY_BUFFER);
                {
                    IndexVBO.Upload(GL_ELEMENT_ARRAY_BUFFER, IndexBuffer);
                }
                IndexVBO.Unbind(GL_ELEMENT_ARRAY_BUFFER);
            }
            
            if( !Texture.isEmpty() && TextureFile == null){
                try{
                    InputStream textureInputStream = new TFileInputStream(new TFile(Texture));
                    TextureFile = TextureLoader.getTexture("JPG", textureInputStream);
                }catch( Exception e ){
                    System.err.println( e );
                }
            }

            DataVBO.Bind(GL_ARRAY_BUFFER);
            IndexVBO.Bind(GL_ELEMENT_ARRAY_BUFFER);

            if (TextureFile != null) {
                glEnable(GL_TEXTURE_2D);
                TextureFile.bind();
                glTexEnvi(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, GL_REPLACE);

                glEnableClientState(GL_TEXTURE_COORD_ARRAY);
            }

            glPushMatrix();
            {
                glMultMatrix( Transformation );
                
                glEnableClientState(GL_VERTEX_ARRAY);
                glEnableClientState(GL_NORMAL_ARRAY);

                glVertexPointer(3, GL_FLOAT, 32, 0);
                glNormalPointer(GL_FLOAT, 32, 12);
                glTexCoordPointer(2, GL_FLOAT, 32, 24);

                glDrawElements(GL_TRIANGLES, Indicies, GL_UNSIGNED_INT, 0L );

                glDisableClientState(GL_VERTEX_ARRAY);
                glDisableClientState(GL_NORMAL_ARRAY);
            }
            glPopMatrix();

            if (TextureFile != null) {
                glDisableClientState(GL_TEXTURE_COORD_ARRAY);
                glDisable(GL_TEXTURE_2D);
            }

            DataVBO.Unbind(GL_ARRAY_BUFFER);
        }
    }
    
    class Cloud{
        float PointScale;
        FloatBuffer Transformation = BufferUtils.createFloatBuffer(16);
        
        int Verticies;
        String Vertex;
        
        private ByteBuffer DataBuffer = null;
        private VertexBufferObject DataVBO = null;
        
        public void SetVertex( String Path ){
            Vertex = Path;
            
            try{
                System.out.println( "Reading Cloud File: " + Vertex );
                TFile vFile = new TFile(Vertex);
                DataInputStream vIn = new DataInputStream(new TFileInputStream(vFile));
                byte[] vInput = new byte[(int)vFile.length()];
                vIn.readFully(vInput);
                DataBuffer = BufferUtils.createByteBuffer(vInput.length);
                DataBuffer.put(vInput);
                DataBuffer.rewind();
                vIn.close();
            }catch( Exception e ){

            }
        }
        
        public void Draw(){
            if( DataVBO == null && DataBuffer != null ){
                DataVBO = new VertexBufferObject();
                DataVBO.Create();
                DataVBO.Bind(GL_ARRAY_BUFFER);
                {
                    DataVBO.Upload(GL_ARRAY_BUFFER, DataBuffer);
                }
                DataVBO.Unbind(GL_ARRAY_BUFFER);   
            }
            
            DataVBO.Bind(GL_ARRAY_BUFFER);
            
            glPointSize(PointScale);
            
            glPushMatrix();
            {
                glMultMatrix( Transformation );

                glEnableClientState(GL_VERTEX_ARRAY);
                glEnableClientState(GL_COLOR_ARRAY);

                glVertexPointer(3, GL_FLOAT, 28, 0);
                glColorPointer(3, GL_FLOAT, 28, 12);

                glDrawArrays(GL_POINTS, 0, Verticies);

                glDisableClientState(GL_VERTEX_ARRAY);
                glDisableClientState(GL_COLOR_ARRAY);
            }
            glPopMatrix();
            
            DataVBO.Unbind(GL_ARRAY_BUFFER);
        }
    }
    
    class Model{
        String Title, Path;
        float Radius;
        Vector3f Center;
        Vector4f View;
        
        List<Mesh> Meshes = new ArrayList<Mesh>();
        List<Cloud> Clouds = new ArrayList<Cloud>();
    }
    
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
