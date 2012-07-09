package org.tqdev.visarray.model;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import org.lwjgl.BufferUtils;
import org.newdawn.slick.opengl.Texture;
import org.newdawn.slick.opengl.TextureLoader;
import org.tqdev.visarray.opengl.VertexBufferObject;

import de.schlichtherle.truezip.file.TFile;
import de.schlichtherle.truezip.file.TFileInputStream;
import de.schlichtherle.truezip.io.Streams;

/**
 * @author James Sweet
 *
 */
public class Mesh{
	public FloatBuffer Transformation = BufferUtils.createFloatBuffer(16);
    
    public int Verticies, Indicies;
    public String Texture = "", Vertex, Index;
    
    private Texture TextureFile = null;
    
    private ByteBuffer DataBuffer = null;
    private VertexBufferObject DataVBO = null;
    
    private ByteBuffer IndexBuffer = null;
    private VertexBufferObject IndexVBO = null;
    
    public void SetVertex( String Path ){
        Vertex = Path;
        
        try{
        	ByteArrayOutputStream data = new ByteArrayOutputStream();
        	Streams.copy( new TFileInputStream(new TFile(Vertex)), data);
            DataBuffer = BufferUtils.createByteBuffer(data.size());
            DataBuffer.put(data.toByteArray());
            DataBuffer.rewind();
        }catch( Exception e ){

        }
    }
    
    public void SetIndex( String Path ){
        Index = Path;
        
        try{
            ByteArrayOutputStream data = new ByteArrayOutputStream();
        	Streams.copy( new TFileInputStream(new TFile(Index)), data);
        	IndexBuffer = BufferUtils.createByteBuffer(data.size());
        	IndexBuffer.put(data.toByteArray());
        	IndexBuffer.rewind();
        }catch( Exception e ){

        }
    }
    
    private static int call = 0;
    
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
        	if( call == 100 ){
	            try{
	            	System.out.println( "Reading Texture: " + Texture );
	                InputStream textureInputStream = new TFileInputStream(new TFile(Texture));
	                TextureFile = TextureLoader.getTexture("JPG", textureInputStream);
	            }catch( Exception e ){
	                System.err.println( e );
	            }
            	call = 0;
        	}else{
        		call++;
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