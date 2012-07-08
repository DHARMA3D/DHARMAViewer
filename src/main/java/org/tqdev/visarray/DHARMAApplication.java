package org.tqdev.visarray;

import de.schlichtherle.truezip.file.TArchiveDetector;
import de.schlichtherle.truezip.file.TConfig;
import de.schlichtherle.truezip.file.TFile;
import de.schlichtherle.truezip.file.TVFS;
import de.schlichtherle.truezip.fs.archive.zip.JarDriver;
import de.schlichtherle.truezip.io.Streams;
import de.schlichtherle.truezip.socket.sl.IOPoolLocator;
import java.awt.Font;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import org.lwjgl.BufferUtils;
import org.lwjgl.input.Mouse;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;

import static org.lwjgl.opengl.GL11.*;
import org.newdawn.slick.Color;
import org.newdawn.slick.UnicodeFont;
import org.newdawn.slick.font.effects.ColorEffect;
import org.newdawn.slick.util.xml.XMLParser;
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
    private UnicodeFont font = new UnicodeFont(new Font("Times New Roman", Font.BOLD, 18));
    
    // Rendering
    private Processor ProcessData;
    
    // Camera
    private float Zoom = 0.0f;
    private Vector2f Rotation = new Vector2f();
    
    public void appletInit( List<String> parameters ) {
    	TConfig.get().setArchiveDetector( new TArchiveDetector( "dhz", new JarDriver(IOPoolLocator.SINGLETON)));
        
    	List<String> requested = Parameters();
		for (int i = 0; i < requested.size(); i++) {
			String param = requested.get(i);
			
			System.out.println( "Parameter: " + param + " = " + parameters.get(i) );
		}
		
        try{ 
        	if( parameters.size() == 0 ){
        		System.err.println( "No arguments passed. Exiting." );
        		System.exit(1);
        	}else{
	            if( !parameters.get(0).isEmpty() ){
	            	ProcessData = new Processor( parameters.get(0) );
	            }
        	}
        }catch( Exception e ){
            System.err.println( "Error: " + e );
        }
        
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
	            	ProcessData = new Processor( arguments[0] );
	            }
        	}
        }catch( Exception e ){
            System.err.println( "Error: " + e );
        }
        
        init();
    }
    
    public void destroy(){
    	try {
			ProcessData.join();
		} catch (InterruptedException e) {
			System.err.println( e );
		}
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
        
        if( !ProcessData.isAlive() && !ProcessData.isProcessed() ) ProcessData.start();

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
        	if( ProcessData.isProcessed() ){
        		List<Model> models = ProcessData.Parser().Models;
        	
	        	Matrix4f transform = new Matrix4f();
	        	transform.translate( new Vector3f( 0.0f, 0.0f, Zoom-(2*models.get(0).Radius) ) );
	        	transform.rotate(Rotation.x, new Vector3f( 0.0f, 1.0f, 0.0f ) );
	        	transform.rotate(Rotation.y, new Vector3f( -1.0f, 0.0f, 0.0f ) );
	        	
	        	FloatBuffer matrix = BufferUtils.createFloatBuffer(16);
	        	transform.store(matrix);
	        	matrix.rewind();
	
	        	glMultMatrix( matrix );
	        	
	            for( Model m: models ){
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
        }
        glPopMatrix();
    }

    private void render2d() {
        mViewport.orthographic();

        glEnable( GL_TEXTURE_2D );
        {
        	if( !ProcessData.isDownloaded() ){
        		String string = "Downloading Model";
        		font.drawString((mViewport.width() / 2)-(font.getWidth(string)/2), (mViewport.height() / 2)-(font.getHeight(string)/2), string);
        	}
        	if( !ProcessData.isProcessed() ){
        		String string = "Processing Model";
        		font.drawString((mViewport.width() / 2)-(font.getWidth(string)/2), (mViewport.height() / 2)-(font.getHeight(string)/2), string);
        	}
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
 
	public List<String> Parameters() {
		List<String> retVal = new ArrayList<String>();
		
		retVal.add( "url" );
		
		return retVal;
	}
	
	public class Processor extends Thread{
		private final String From;
		private boolean Downloaded, Processed;
		private XMLParse Parser;
		
		public Processor( String address ){
			From = address;
			Downloaded = false;
			Parser = new XMLParse();
			
			this.setPriority(Thread.MIN_PRIORITY);
		}
		
		synchronized public boolean isDownloaded(){
			return Downloaded;
		}
		
		synchronized public boolean isProcessed(){
			return Processed;
		}
		
		public XMLParse Parser(){
			return Parser;
		}
		
		public void run() {
	        String To = System.getProperty("java.io.tmpdir") + From.substring( From.lastIndexOf("/") + 1 );
	        
	        System.out.println("Downloading: " + From + " to " + To );
	        
	        try{
	        	Streams.copy(new URL(From).openConnection().getInputStream(), new FileOutputStream(To));
	        }catch( Exception e ){
	        	System.err.println( "Download Error: " + e );
	        }
	        
	        System.out.println("Downloaded: " + From );
            Downloaded = true;
	        
	        try {
	            TFile list[] = new TFile( To ).listFiles();
	            for (int i = 0; i < list.length; i++) {
	                if (list[i].getName().contains(".xml")) {
	                    try{
	                        Parser.Parse(list[i]);
	                    }catch( Exception ex ){
	                    	System.out.println( "Process Error: " + ex );
	                    }
	                }
	            }
	            
	            Processed = true;
	        } catch (Exception exception) {
	            System.out.println( "Thread Error: " + exception );
	        }
		}
		
	}
}
