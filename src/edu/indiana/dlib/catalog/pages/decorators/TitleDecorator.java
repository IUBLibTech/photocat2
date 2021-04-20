package edu.indiana.dlib.catalog.pages.decorators;

import org.apache.click.Page;

public class TitleDecorator extends Page {
  public String title;
  private Page p;

  public TitleDecorator(String title, Page p) {
    this.title = title;
    this.p = p;
  }

  public void onRender() {
    p.onRender();
    // Any additional logic for rending the title goes here

    // Render the title
    super.onRender();
  }
}
