package edu.indiana.dlib.catalog.pages.decorators;

import org.apache.click.Page;

public class BreadcrumbDecorator extends Page {
  public String breadcrumb;
  private Page p;

  public BreadcrumbDecorator(String breadcrumb, Page p) {
    this.breadcrumb = breadcrumb;
    this.p = p;
  }

  public void  onRender() {
    p.onRender();
    // Additional logic for rendering the breadcrumb goes here
    super.onRender();
  }
}
