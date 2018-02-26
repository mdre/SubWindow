package net.awesomecontrols.subwindow.demo;

import net.awesomecontrols.subwindow.SubWindow;

import javax.servlet.annotation.WebServlet;

import com.vaadin.annotations.Theme;
import com.vaadin.annotations.Title;
import com.vaadin.annotations.VaadinServletConfiguration;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinServlet;
import com.vaadin.ui.HorizontalSplitPanel;
import com.vaadin.ui.Label;
import com.vaadin.ui.Panel;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.VerticalSplitPanel;
import com.vaadin.ui.Window;
import net.awesomecontrols.subwindow.SubWindowDesktop;

@Theme("demo")
@Title("SubWindow Add-on Demo")
@SuppressWarnings("serial")
public class DemoUI extends UI
{

    @WebServlet(value = "/*", asyncSupported = true)
    @VaadinServletConfiguration(productionMode = false, ui = DemoUI.class)
    public static class Servlet extends VaadinServlet {
    }

    @Override
    protected void init(VaadinRequest request) {

        Window w = new Window("Window");
        w.setWidth("300px");
        w.setHeight("200px");
        
        Panel pw = new Panel();
        pw.setSizeFull();
        VerticalLayout vlw = new VerticalLayout();
        vlw.addComponent(new Label("Label 1"));
        vlw.addComponent(new Label("Label 2"));
        vlw.addComponent(new Label("Label 3"));
        vlw.setSizeFull();
        pw.setContent(vlw);
        w.setContent(pw);
        
        this.addWindow(w);
        
        Window w2 = new Window("Window 2");
        w2.setWidth("300px");
        w2.setHeight("200px");
        
        this.addWindow(w2);
        
        
        
        Panel superior = new Panel();
        Panel inferior = new Panel();
        
        
        
        // Initialize our new UI component
        final SubWindow subw = new SubWindow("SubWindow");
        subw.setWidth("300px");
        subw.setHeight("200px");
        
        Panel p = new Panel();
        
        VerticalLayout vl = new VerticalLayout();
        vl.addComponent(new Label("Label 1"));
        vl.addComponent(new Label("Label 2"));
        vl.addComponent(new Label("Label 3"));
        
        p.setContent(vl);
        
        subw.setContent(p);
        
        
        
        final SubWindow subw2 = new SubWindow("SubWindow 2");
        subw2.setWidth("300px");
        subw2.setHeight("200px");
        
        Panel p2 = new Panel();
        p2.setSizeFull();
        VerticalLayout vl2 = new VerticalLayout();
        vl2.addComponent(new Label("Label 1"));
        vl2.addComponent(new Label("Label 2"));
        vl2.addComponent(new Label("Label 3"));
        vl2.setSizeFull();
        p2.setContent(vl2);
        
        subw2.setContent(p2);
        subw2.setPosition(50, 60);
        
        
        
        
        // Show it in the middle of the screen
        SubWindowDesktop swd = new SubWindowDesktop();
        
        swd.setSizeFull();
//        layout.setMargin(false);
//        layout.setSpacing(false);
        swd.addSubWindow(subw);
        swd.addSubWindow(subw2);
        
        HorizontalSplitPanel hsp = new HorizontalSplitPanel(new Panel(),swd);
        hsp.setSplitPosition(200, Unit.PIXELS);
        
        VerticalSplitPanel vsp = new VerticalSplitPanel(superior,hsp);
        vsp.setSplitPosition(200, Unit.PIXELS);
        
//        layout.setComponentAlignment(component, Alignment.MIDDLE_CENTER);
        setContent(vsp);
    }
}
