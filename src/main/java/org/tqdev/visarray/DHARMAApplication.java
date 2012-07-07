package org.tqdev.visarray;

import de.schlichtherle.truezip.file.TArchiveDetector;
import de.schlichtherle.truezip.file.TConfig;
import de.schlichtherle.truezip.file.TFile;
import de.schlichtherle.truezip.file.TVFS;
import de.schlichtherle.truezip.fs.archive.zip.JarDriver;
import de.schlichtherle.truezip.socket.sl.IOPoolLocator;
import java.awt.Font;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.FloatBuffer;
import org.lwjgl.BufferUtils;
import org.lwjgl.input.Mouse;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;

import static org.lwjgl.opengl.GL11.*;
import org.newdawn.slick.Color;
import org.newdawn.slick.UnicodeFont;
import org.newdawn.slick.font.effects.ColorEffect;
import org.tqdev.visarray.XMLParse.Cloud;
import org.tqdev.visarray.XMLParse.Mesh;
import org.tqdev.visarray.XMLParse.Model;
import org.tqdev.visarray.opengl.Viewport;

/**
 *
 * @author Omegaice
 */
public class DHARMAApplication implements VisApplication {
    
    private double mFrameEnd = 0.0f, mFrameTime = 0.0f;
    private Viewport mViewport = new Viewport();
    private XMLParse Parser = new XMLParse();
    private UnicodeFont font = new UnicodeFont(new Font("Times New Roman", Font.BOLD, 18));
    
    // Camera
    private float Zoom = 0.0f;
    private Vector2f Rotation = new Vector2f();

    public void appletInit() {
        init();
    } 

    public void applicationInit(String[] arguments) {
        TConfig.get().setArchiveDetector( new TArchiveDetector( "dhz", new JarDriver(IOPoolLocator.SINGLETON)));
        
        try{ 
        	if( arguments.length == 0 ){
        		System.err.println( "No arguments passed. Exiting." );
        		System.exit(1);
        	}else{
	            if( !arguments[0].isEmpty() ){
	                String toOpen = arguments[0];
	                if( arguments[0].contains("http") ){
	                    String filename = System.getProperty("java.io.tmpdir") + arguments[0].substring( arguments[0].lastIndexOf("/") + 1 );
	                    download(arguments[0], filename);
	                    toOpen = filename;
	                }
	                
	                TFile archive = new TFile( toOpen );
	                archive.setReadOnly();
	
	                TFile list[] = archive.listFiles();
	                for (int i = 0; i < list.length; i++) {
	                    if (list[i].getName().contains(".xml")) {
	                        try{
	                            Parser = new XMLParse();
	                            Parser.Parse(list[i]);
	                        }catch( Exception ex ){
	
	                        }
	                    }
	                }
	            }
        	}
        }catch( Exception e ){
            System.err.println( "Error: " + e );
        }
        
        init();
    }
    
    private void init(){
    	try{
	    	font.addAsciiGlyphs(); 
	    	font.getEffects().add(new ColorEffect(java.awt.Color.white));
	    	font.loadGlyphs();
    	}catch( Exception e ){
    		System.err.println( "Error Loading Font: " + e );
    	}
    	
        glShadeModel(GL_SMOOTH);
        glEnable(GL_LIGHTING);
        glEnable(GL_COLOR_MATERIAL);
        
        FloatBuffer ambient = BufferUtils.createFloatBuffer(4);
        ambient.put( 0.5f );
        ambient.put( 0.5f );
        ambient.put( 0.5f );
        ambient.put( 1.0f );
        ambient.rewind();
        
        glLightModel(GL_LIGHT_MODEL_AMBIENT, ambient );

        glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        glClearDepth(1.0f);

        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
    }

    public void resize(final int width, final int height) {
        glViewport(0, 0, width, height);
        mViewport.size(width, height);
    }

    public void render() {
        mFrameTime = System.currentTimeMillis() - mFrameEnd;

        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        
        processInput();

        render3d();
        render2d();

        mFrameEnd = System.currentTimeMillis();
    }
    
    private void processInput(){
        if (Mouse.isButtonDown(0)) {
            int xDiff = Mouse.getDX();
            int yDiff = Mouse.getDY();
            
            // Dragged
            if( xDiff != 0 || yDiff != 0 ){
            	Rotation.x += 0.01 * xDiff;
            	Rotation.y += 0.01 * yDiff;
            }
        }
        
        int wDiff = Mouse.getDWheel();
        if( wDiff != 0 ){
        	Zoom += 0.01 * wDiff;
        	mViewport.clip(0.1f, 100.0f-Zoom );
        }
    }

    private void render3d() {
        mViewport.perspective();
        
        glPushMatrix();
        {
        	Matrix4f transform = new Matrix4f();
        	transform.translate( new Vector3f( 0.0f, 0.0f, Zoom-Parser.Models.get(0).Radius ) );
        	transform.rotate(Rotation.x, new Vector3f( 0.0f, 1.0f, 0.0f ) );
        	transform.rotate(Rotation.y, new Vector3f( -1.0f, 0.0f, 0.0f ) );
        	
        	FloatBuffer matrix = BufferUtils.createFloatBuffer(16);
        	transform.store(matrix);
        	matrix.rewind();

        	glMultMatrix( matrix );
        	
            for( Model m: Parser.Models ){
                glRotatef( 270.0f, 1.0f, 0.0f, 0.0f );
                glTranslatef(-m.Center.x, -m.Center.y, -m.Center.z);
                
                for( Mesh mesh: m.Meshes ){
                    mesh.Draw();
                }

                glDisable( GL_LIGHTING );
                {
                    for( Cloud cloud: m.Clouds ){
                        cloud.Draw();
                    }
                }
                glEnable( GL_LIGHTING );
            }
        }
        glPopMatrix();
    }

    private void render2d() {
        mViewport.orthographic();

        glEnable( GL_TEXTURE_2D );
        {
            String renderer = String.format("Renderer: %.0fms %.0ffps", CalcAverageTick( mFrameTime ), 1000 / CalcAverageTick( mFrameTime ));
            font.drawString(mViewport.width() - font.getWidth(renderer), mViewport.height() - font.getHeight(renderer), renderer);
        }
        glDisable( GL_TEXTURE_2D );
    }
    
    int tickindex = 0;
    double ticksum = 0;
    double ticklist[] = new double[100];

    private double CalcAverageTick(double newtick) {
        ticksum -= ticklist[tickindex];
        ticksum += newtick;
        ticklist[tickindex] = newtick;
        if (++tickindex == 100) {
            tickindex = 0;
        }

        /* return average */
        return ( ticksum / 100.0f);
    }
    
    public static void download(String address, String localFileName) {
        OutputStream out = null;
        InputStream in = null;
        
        System.out.println("Downloading: " + address + " to " + localFileName );
        try {
            URL url = new URL(address);
            out = new BufferedOutputStream(new FileOutputStream(localFileName));
            
            in = url.openConnection().getInputStream();

            // Get the data
            byte[] buffer = new byte[1024];
            
            int numRead;
            while ((numRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, numRead);
            }
        } catch (Exception exception) {
            System.out.println( exception );
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
                if (out != null) {
                    out.close();
                }
            } catch (IOException ioe) {
                System.out.println( ioe );
            }
        }
    }
}
