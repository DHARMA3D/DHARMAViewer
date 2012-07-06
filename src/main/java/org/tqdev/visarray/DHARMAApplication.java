package org.tqdev.visarray;

import java.awt.Font;
import static org.lwjgl.opengl.GL11.*;
import org.newdawn.slick.Color;
import org.newdawn.slick.TrueTypeFont;
import org.tqdev.visarray.opengl.Viewport;

/**
 *
 * @author Omegaice
 */
public class DHARMAApplication implements VisApplication {
    
    private double mFrameEnd = 0.0f, mFrameTime = 0.0f;
    private Viewport mViewport = new Viewport();
    private TrueTypeFont font = new TrueTypeFont(new Font("Times New Roman", Font.BOLD, 18), true);

    public void appletInit() {
        init();
    }

    public void applicationInit(String[] arguments) {
        init();
    }
    
    private void init(){
        glEnable(GL_TEXTURE_2D);
        glShadeModel(GL_SMOOTH);
        glDisable(GL_DEPTH_TEST);
        glDisable(GL_LIGHTING);

        glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        glClearDepth(1);

        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
    }

    public void resize(final int width, final int height) {
        glViewport(0, 0, width, height);
        mViewport.size(width, height);
    }

    public void render() {
        mFrameTime = System.currentTimeMillis() - mFrameEnd;

        glClear(GL_COLOR_BUFFER_BIT);

        render3d();
        render2d();

        mFrameEnd = System.currentTimeMillis();
    }

    private void render3d() {
        mViewport.perspective();
    }

    private void render2d() {
        mViewport.orthographic();

        String renderer = String.format("Renderer: %.0fms %.0ffps", CalcAverageTick( mFrameTime ), 1000 / CalcAverageTick( mFrameTime ));
        font.drawString(mViewport.width() - font.getWidth(renderer), mViewport.height() - font.getHeight(renderer), renderer, Color.white);
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
}
