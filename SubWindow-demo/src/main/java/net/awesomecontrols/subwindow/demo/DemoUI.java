package net.awesomecontrols.subwindow.demo;

import com.vaadin.annotations.Theme;
import com.vaadin.annotations.Title;
import com.vaadin.annotations.VaadinServletConfiguration;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinServlet;
import com.vaadin.ui.Button;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.HorizontalSplitPanel;
import com.vaadin.ui.Label;
import com.vaadin.ui.Panel;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.VerticalSplitPanel;
import javax.servlet.annotation.WebServlet;
import net.awesomecontrols.subwindow.SubWindow;
import net.awesomecontrols.subwindow.SubWindowDesktop;

@Theme("demo")
@Title("SubWindow Add-on Demo")
@SuppressWarnings("serial")
public class DemoUI extends UI
{
    SubWindowDesktop swd;
    int subwCount = 1;
    
    @WebServlet(value = "/*", asyncSupported = true)
    @VaadinServletConfiguration(productionMode = false, ui = DemoUI.class)
    public static class Servlet extends VaadinServlet {
    }

    @Override
    protected void init(VaadinRequest request) {

//        Window w = new Window("Window");
//        w.setWidth("300px");
//        w.setHeight("200px");
//        
//        Panel pw = new Panel();
//        pw.setSizeFull();
//        VerticalLayout vlw = new VerticalLayout();
//        vlw.addComponent(new Label("Label 1"));
//        vlw.addComponent(new Label("Label 2"));
//        vlw.addComponent(new Label("Label 3"));
//        vlw.setSizeFull();
//        pw.setContent(vlw);
//        w.setContent(pw);
//        
//        this.addWindow(w);
//        
//        Window w2 = new Window("Window 2");
//        w2.setWidth("300px");
//        w2.setHeight("200px");
//        
//        this.addWindow(w2);
//        
        
        
        Panel superior = new Panel();
        Panel inferior = new Panel();
        
        Button addSW = new Button("Agregar subwindow.", new Button.ClickListener() {
            @Override
            public void buttonClick(Button.ClickEvent event) {
                // Initialize our new UI component
                SubWindow subw = new SubWindow("SubWindow "+subwCount);
                subwCount++;
                
                subw.setWidth("300px");
                subw.setHeight("200px");

                Panel p = new Panel();

                VerticalLayout vl = new VerticalLayout();
                vl.addComponent(new Label("Label 1"));
                vl.addComponent(new Label("Label 2"));
                vl.addComponent(new Label("Label 3"));

                p.setContent(vl);

                subw.setContent(p);
                
                swd.addSubWindow(subw);
            }
        });
        HorizontalLayout hlSuperior = new HorizontalLayout();
        hlSuperior.addComponent(addSW);
        superior.setContent(hlSuperior);
        
        
        // Show it in the middle of the screen
        swd = new SubWindowDesktop();
        swd.setSizeFull();
        
        HorizontalSplitPanel hsp = new HorizontalSplitPanel(new Panel(),swd);
        hsp.setSplitPosition(100, Unit.PIXELS);
        
        VerticalSplitPanel vsp2 = new VerticalSplitPanel(hsp, new Panel());
        vsp2.setSplitPosition(50, Unit.PIXELS,true);
        
        VerticalSplitPanel vsp = new VerticalSplitPanel(superior,vsp2);
        vsp.setSplitPosition(100, Unit.PIXELS);
        
        
//        layout.setComponentAlignment(component, Alignment.MIDDLE_CENTER);
        setContent(vsp);
    }
}
