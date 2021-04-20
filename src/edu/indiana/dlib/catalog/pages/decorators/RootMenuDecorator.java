package edu.indiana.dlib.catalog.pages.decorators;

import org.apache.click.Page;
import org.apache.click.extras.control.Menu;

public class RootMenuDecorator extends Page {
  public Menu rootMenu;
  private Page p;

  public RootMenuDecorator(Menu rootMenu, Page p) {
    this.rootMenu = rootMenu;
    this.p = p;
  }

  public void onRender() {
    p.onRender();
    // Additional logic for rendering the root menu
    super.onRender();
  }
}
