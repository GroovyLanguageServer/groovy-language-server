package net.prominic.groovyls.util;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.codehaus.groovy.ast.AnnotatedNode;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.CompileUnit;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.control.Phases;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import net.prominic.groovyls.compiler.control.GroovyLSCompilationUnit;
import net.prominic.groovyls.config.CompilationUnitFactory;

class GroovyDocUtilsTests {

  private static final String PATH_WORKSPACE = "./build/test_workspace/";

	@Test
	void testDidRemoveSingleLineComment() {
    String expected = "SomeMethod";
		String docString = "/** " + expected + " */";
		
    String result = GroovyDocUtils.removeComment(docString);

		Assertions.assertEquals(expected, result);
	}

  @Test
	void testDidRemoveMultiLineComment() {
    String expected = "SomeMethod";

    StringBuilder docString = new StringBuilder();

    docString.append("/**\n");
    docString.append("* " + expected + "\n");
    docString.append("*/");

    String result = GroovyDocUtils.removeComment(docString.toString());

		Assertions.assertEquals(expected + "\n", result.toString());
	}

  @Test
	void testDidConvertParams() {
    String text = "@param MyParam This is a test param.";
    String expected = "- *param* **MyParam** - This is a test param.";

    String result = GroovyDocUtils.convertParams(text);

		Assertions.assertEquals(expected, result);
	}

  @Test
	void testDidNotConvertParams() {
    String text = "SomeText";
    String expected = text;

    String result = GroovyDocUtils.convertParams(text);

		Assertions.assertEquals(expected, result);
	}

  @Test
	void testDidConvertReturn() {
    String text = "@return Some Value";
    String expected = "- *return* - Some Value";

    String result = GroovyDocUtils.convertReturn(text);

		Assertions.assertEquals(expected, result);
	}

  @Test
	void testDidNotConvertReturn() {
    String text = "SomeText";
    String expected = text;

    String result = GroovyDocUtils.convertParams(text);

		Assertions.assertEquals(expected, result);
	}

  @Test
	void testDidRemoveTags() {
    String text = "@see Some Value";
    String expected = "";

    String result = GroovyDocUtils.removeTags(text);

		Assertions.assertEquals(expected, result);
	}

  @Test
	void testDidNotRemoveTags() {
    String text = "SomeText";
    String expected = text;

    String result = GroovyDocUtils.convertParams(text);

		Assertions.assertEquals(expected, result);
	}

  @Test
	void testDidConvertNewLines() {
    String text = "\n";
    String expected = "  " + text;

    String result = GroovyDocUtils.convertNewLines(text);

		Assertions.assertEquals(expected, result);
	}

  @Test
	void testDidNotConvertNewLines() {
    String text = "SomeText";
    String expected = text;

    String result = GroovyDocUtils.convertNewLines(text);

		Assertions.assertEquals(expected, result);
	}

  @Test
	void testDidConvertToMarkdown() {
    
    final String docString = "/**\n"
    + "* Returns an Image object that can then be painted on the screen.\n"
    + "* The url argument must specify an absolute <a href=\"#{@link}\">{@link URL}</a>. The name\n"
    + "* argument is a specifier that is relative to the url argument.\n"
    + "*\n"
    + "* This method always returns immediately, whether or not the\n"
    + "* image exists. When this applet attempts to draw the image on\n"
    + "* the screen, the data will be loaded. The graphics primitives\n"
    + "* that draw the image will incrementally paint on the screen.\n"
    + "*\n"
    + "* @param  url  an absolute URL giving the base location of the image\n"
    + "* @param  name the location of the image, relative to the url argument\n"
    + "* @return      the image at the specified URL\n"
    + "* @see         Image\n"
    + "*/";

    final String expected = "Returns an Image object that can then be painted on the screen.  \n"
	 + "The url argument must specify an absolute <a href=\"#{@link}\">{@link URL}</a>. The name  \n"
	 + "argument is a specifier that is relative to the url argument.  \n  \n"
	 + "This method always returns immediately, whether or not the  \n"
	 + "image exists. When this applet attempts to draw the image on  \n"
	 + "the screen, the data will be loaded. The graphics primitives  \n"
	 + "that draw the image will incrementally paint on the screen.  \n  \n"
	 + "- *param* **url** - an absolute URL giving the base location of the image  \n"
	 + "- *param* **name** - the location of the image, relative to the url argument  \n"
	 + "- *return* - the image at the specified URL  \n";

		
    String result = GroovyDocUtils.convertToMarkdown(docString);

		Assertions.assertEquals(expected, result);
	}

  @Test
	void testDidNotConvertToMarkdown() {
    
    String text = "Some Value";
    String expected = text;

		
    String result = GroovyDocUtils.convertToMarkdown(text);

		Assertions.assertEquals(expected, result);
	}

  @Test
	void testDidAddDocString() {
  
    CompilationUnitFactory cf = new CompilationUnitFactory();

    FileContentsTracker fileContentsTracker = new FileContentsTracker();

    Path workspaceRoot = Paths.get(System.getProperty("user.dir")).resolve(PATH_WORKSPACE);

    GroovyLSCompilationUnit cp = cf.create(workspaceRoot, fileContentsTracker);

    StringBuilder funcDefinition = new StringBuilder();

    funcDefinition.append("class MyClass {\n");
    funcDefinition.append("  /** SomeFunction */\n");
    funcDefinition.append("  def myFunction() {}\n");
    funcDefinition.append("}");

    cp.addSource("test.groovy", funcDefinition.toString());
    cp.compile(Phases.SEMANTIC_ANALYSIS);
    CompileUnit ast = cp.getAST();
    ClassNode parent = ast.getClasses().get(0);
    MethodNode child = parent.getMethods().get(0);

    String result = GroovyDocUtils.getDocString(child);

		Assertions.assertNotNull(result);
	}

  @Test
	void testDidNotAddDocString() {

    AnnotatedNode node = new AnnotatedNode();
    String expected = null;

    String docString = GroovyDocUtils.getDocString(node);

		Assertions.assertEquals(expected, docString);
	}

}