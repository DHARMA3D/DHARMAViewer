package org.tqdev.visarray;

import static org.lwjgl.opengl.GL11.*;

import java.awt.Font;
import java.io.FileOutputStream;
import java.net.URL;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import org.lwjgl.BufferUtils;
import org.lwjgl.input.Mouse;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;
import org.newdawn.slick.UnicodeFont;
import org.newdawn.slick.font.effects.ColorEffect;
import org.tqdev.visarray.model.*;
import org.tqdev.visarray.opengl.Viewport;

import de.schlichtherle.truezip.file.TArchiveDetector;
import de.schlichtherle.truezip.file.TConfig;
import de.schlichtherle.truezip.file.TFile;
import de.schlichtherle.truezip.fs.archive.zip.JarDriver;
import de.schlichtherle.truezip.io.Streams;
import de.schlichtherle.truezip.socket.sl.IOPoolLocator;

/**
 *
 * @author James Sweet
 */
public class DHARMAApplication implements VisApplication {
    
    private double mFrameEnd = 0.0f, mFrameTime = 0.0f;
    private Viewport mViewport = new Viewport();
    private UnicodeFont font = new UnicodeFont(new Font("Times New Roman", Font.BOLD, 18));
    
    // Rendering
    private Processor ProcessData;
    private Vector3f Background = new Vector3f( 0.0f, 0.0f, 0.0f );
    
    // Camera
    private float Radius = 0.0f;
    private float Zoom = 0.0f;
    private Vector2f Rotation = new Vector2f();
    
    public void initialize( List<String> parameters ) {
    	TConfig.get().setArchiveDetector( new TArchiveDetector( "dhz", new JarDriver(IOPoolLocator.SINGLETON)));
        
    	List<String> requested = getParameters();
		for (int i = 0; i < requested.size(); i++) {
			String param = requested.get(i);
			
			System.out.println( "Parameter: " + param + " = " + parameters.get(i) );
		}
		
        try{ 
        	if( parameters.isEmpty() ){
        		System.err.println( "No arguments passed. Exiting." );
        		System.exit(1);
        	}else{
	            if( !parameters.get(0).isEmpty() ){
	            	ProcessData = new Processor( parameters.get(0) );
	            }
	            if( !parameters.get(1).isEmpty() ){
	            	String[] part = parameters.get(1).trim().split( "\\s+", 3 );
	            	Background.x = Float.parseFloat( part[0] );
	            	Background.y = Float.parseFloat( part[1] );
	            	Background.z = Float.parseFloat( part[2] );
	            }
        	}
        }catch( Exception e ){
            System.err.println( "Error: " + e );
        }
        
        try{
	    	font.addAsciiGlyphs(); 
	    	font.getEffects().add(new ColorEffect(java.awt.Color.white));
	    	font.loadGlyphs();
    	}catch( Exception e ){
    		System.err.println( "Error Loading Font: " + e );
    	}
    	
        glShadeModel(GL_SMOOTH);
        glEnable(GL_LIGHTING);
        glEnable(GL_DEPTH_TEST);
        glEnable(GL_COLOR_MATERIAL);
        
        FloatBuffer ambient = BufferUtils.createFloatBuffer(4);
        ambient.put( 0.5f );
        ambient.put( 0.5f );
        ambient.put( 0.5f );
        ambient.put( 1.0f );
        ambient.rewind();
        
        glLightModel(GL_LIGHT_MODEL_AMBIENT, ambient );

        glClearColor( Background.x, Background.y, Background.z, 0.0f);
        glClearDepth(1.0f);

        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
    }
    
    public void destroy(){
    	try {
			ProcessData.join();
		} catch (InterruptedException e) {
			System.err.println( e );
		}
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

        mViewport.perspective();
        render3d();
        
        mViewport.orthographic();
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
        
        if (Mouse.isButtonDown(1)) {
        	Rotation.x = 0.0f;
        	Rotation.y = 0.0f;
        	Zoom = 0.0f;
        }
        
        int wDiff = Mouse.getDWheel();
        if( wDiff != 0 ){
        	Zoom += 0.01 * wDiff;
        	mViewport.clip(0.1f, Zoom-Radius );
        }
    }

    private void render3d() {
        glPushMatrix();
        {
        	if( ProcessData.isProcessed() ){
        		List<Model> models = ProcessData.Parser().Models;
        		
        		if( Math.abs( Radius ) < 0.00001f ){
        			Radius = (2*models.get(0).Radius);
        			mViewport.clip(0.1f, Zoom-Radius );
        		}
        	
	        	Matrix4f transform = new Matrix4f();
	        	transform.translate( new Vector3f( 0.0f, 0.0f, Zoom-Radius ) );
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
        glEnable( GL_TEXTURE_2D );
        {
        	if( !ProcessData.isDownloaded() ){
        		String string = "Downloading Model";
        		font.drawString((mViewport.width() / 2)-(font.getWidth(string)/2), (mViewport.height() / 2)-(font.getHeight(string)/2), string);
        	}else if( !ProcessData.isProcessed() ){
        		String string = "Processing Model";
        		font.drawString((mViewport.width() / 2)-(font.getWidth(string)/2), (mViewport.height() / 2)-(font.getHeight(string)/2), string);
        	}
            String renderer = String.format("Renderer: %.2fms %.2ffps", CalcAverageTick( mFrameTime ), 1000 / CalcAverageTick( mFrameTime ));
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
 
	public List<String> getParameters() {
		List<String> retVal = new ArrayList<String>();
		
		retVal.add( "url" );
		retVal.add( "background" );
		
		return retVal;
	}
	
	public String getTitle(){
    	return "DHARMA Viewer";
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

            @Override
            public void run() {
                String path = From;
                
                if( From.contains("http") ){
                    path = System.getProperty("java.io.tmpdir") + From.substring( From.lastIndexOf("/") + 1 );

                    System.out.println("Downloading: " + From + " to " + path );

                    try{
                            Streams.copy(new URL(From).openConnection().getInputStream(), new FileOutputStream(path));
                    }catch( Exception e ){
                            System.err.println( "Download Error: " + e );
                    }

                    System.out.println("Downloaded: " + From );
                }
                Downloaded = true;

                try {
                    TFile list[] = new TFile( path ).listFiles();
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

