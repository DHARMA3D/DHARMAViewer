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
import static org.lwjgl.opengl.GL11.*;
import org.newdawn.slick.Color;
import org.newdawn.slick.TrueTypeFont;
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
    private TrueTypeFont font = new TrueTypeFont(new Font("Times New Roman", Font.BOLD, 18), true);

    public void appletInit() {
        init();
    }

    public void applicationInit(String[] arguments) {
        TConfig.get().setArchiveDetector( new TArchiveDetector( "dhz", new JarDriver(IOPoolLocator.SINGLETON)));
        
        try{ 
            if( !arguments[0].isEmpty() ){
                String toOpen = arguments[0];
                if( arguments[0].contains("http") ){
                    String filename = System.getProperty("java.io.tmpdir") + arguments[0].substring( arguments[0].lastIndexOf("/") + 1 );
                    download(arguments[0], filename);
                    toOpen = filename;
                }

                TFile list[] = new TFile( toOpen ).listFiles();
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

            TVFS.umount();
        }catch( Exception e ){
            System.err.println( e );
        }
        
        init();
    }
    
    private void init(){
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

        render3d();
        render2d();

        mFrameEnd = System.currentTimeMillis();
    }

    private void render3d() {
        mViewport.perspective();
        
        glPushMatrix();
        {
            for( Model m: Parser.Models ){
                glTranslatef( 0.0f, 0.0f, -m.Radius );
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
            font.drawString(mViewport.width() - font.getWidth(renderer), mViewport.height() - font.getHeight(renderer), renderer, Color.white);
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
