package patternFinder;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Map;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;

import gui.OutputWindow;
import patternParser.BasicNode;
import patternParser.Rule;
import patternsGrammar.ParseException;
import patternsGrammar.Parser;
import patternsGrammar.SimpleNode;

public class PAT {

	public static final String FS = System.getProperty("file.separator");
	public static File javaFile = null;

	public static void main(String[] args) {

		if(args.length != 2){
			System.out.println("Usage: PAT <DSL File> <Java File>");
			return;
		}

		String DSLFilePath = args[0];
		String JavaFilePath = args[1];
		
		SimpleNode root = null;
		InputStream input = System.in;
		if(args.length > 0)
			try {
				input = new FileInputStream("input" + FS + DSLFilePath);
				
				Parser parser = new Parser(input);
				root = parser.Start();
				root.dump("");
			} catch (FileNotFoundException | ParseException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
				return;
			}
		
		CompilationUnit cu = null;
		BasicNode startNode = null;
		
		try {
			cu = eclipseAST(new String(Files.readAllBytes(Paths.get("input" + FS + JavaFilePath))).toCharArray());
			startNode = getAST(root);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		ArrayList<BasicNode> rules = startNode.getChildren();
		String[] rulesName = new String[rules.size()]; 
		boolean[] rulesFound = new boolean[rules.size()];
		
		for(int i= 0; i<rules.size();i++){
			System.out.println("Searching in rule: " + rules.get(i).toString());
			
			MyRuleFinder myRuleFinder = findRule(cu, (Rule) rules.get(i));
			rulesName[i] = rules.get(i).getValue().substring(2);
			rulesFound[i] = !myRuleFinder.getCorrespondenciesPositions().isEmpty();
			
		}
	
		System.out.println("Finished");
		System.out.println("Rules Found:");
		for(int i= 0; i<rules.size();i++){
			if(rulesFound[i]){
				System.out.println(rulesName[i]);
			}
		}
		
	}
	
	public static void run(InputStream patterns, File javaFile) throws ParseException{
	
		SimpleNode root = null;
		PAT.javaFile = javaFile;

		
		Parser parser = new Parser(patterns);
		root = parser.Start();
		
		
		root.dump("");
			
		CompilationUnit cu = null;
		BasicNode startNode = null;
		
		try {
			cu = eclipseAST(new String(Files.readAllBytes(javaFile.toPath())).toCharArray());
			startNode = getAST(root);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		ArrayList<BasicNode> rules = startNode.getChildren();
		
		OutputWindow outputWindow = new OutputWindow();
		outputWindow.createAndDisplayGUI(PAT.javaFile);
		
		for(BasicNode rule : rules){
			System.out.println("Searching in rule: " + rule.toString());
			
			MyRuleFinder myRuleFinder = findRule(cu, (Rule) rule);
			String ruleName = rule.getValue().substring(2);
			outputWindow.highlightPatterns(ruleName, myRuleFinder.getCorrespondenciesPositions());
		}
	
		System.out.println("Finished");
		
		
	}
		
	
	private static CompilationUnit eclipseAST(char[] unit){
		
		@SuppressWarnings("deprecation")
		ASTParser parser = ASTParser.newParser(AST.JLS3); 
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		parser.setSource(unit); // set source
		
		parser.setResolveBindings(true); // we need bindings later on
		parser.setBindingsRecovery(true);
		
		Map<String, String> options = JavaCore.getOptions();
		parser.setCompilerOptions(options);
		
		CompilationUnit cu = (CompilationUnit) parser.createAST(null);
		
		return cu;	
		
	}
	
	private static MyRuleFinder findRule(CompilationUnit cu, Rule rule){
		
		if(cu == null)
		{
			System.out.println("Given CompilationUnit is null.");
			return null;
		}
		
		MyRuleFinder myRuleFinder = new MyRuleFinder(cu, rule);
		myRuleFinder.search(cu);
		//System.out.println("Same order:" + myRuleFinder.verifySameOrder());
		//System.out.println("Same parent:" + myRuleFinder.verifySameParent());
		
		return myRuleFinder;
	}
	
	
	public static BasicNode getAST(SimpleNode node) throws Exception {
		
		BasicNode ret = BasicNode.parseFromString(node.toString());
		
		
		for (int i = 0; i < node.jjtGetNumChildren(); i++)
		{
			SimpleNode n = (SimpleNode) node.jjtGetChild(i);
			if(n != null)
			{
				ret.addChild(getAST(n));
			}
		}
		
		return ret;
		
	  }
	
}