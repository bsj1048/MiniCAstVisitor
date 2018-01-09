import java.util.*;

import org.antlr.v4.runtime.tree.TerminalNode;

import Domain.*;
import Domain.Args.*;
import Domain.Decl.*;
import Domain.Expr.*;
import Domain.Param.*;
import Domain.Stmt.*;
import Domain.Type_spec.*;
import Domain.Type_spec.TypeSpecification.Type;

public class MiniCAstVisitor extends MiniCBaseVisitor<MiniCNode> {

	boolean isBinaryOperation(MiniCParser.ExprContext ctx) {
		return !(ctx.getChild(1) instanceof MiniCParser.ExprContext);
	}
	
	boolean isFunctionExpr(MiniCParser.ExprContext ctx) {
		// expr의 child개수가 4일 때만 호출되므로 functions인지 배열인지 확인하는 함수
		return ctx.getChild(1).getText().equals("(");
	}

	@Override
	public Program visitProgram(MiniCParser.ProgramContext ctx) {
		List<Declaration> decls = new ArrayList<Declaration>();

		for (int i = 0; i < ctx.decl().size(); i++) {
			decls.add(visitDecl(ctx.decl(i)));
		}
		return new Program(decls);

	}
	

	@Override
	public Declaration visitDecl(MiniCParser.DeclContext ctx) {

		if (ctx.getChild(0) instanceof MiniCParser.Fun_declContext) {
			// instanceof를 이용한 클래스 타입비교
			return visitFun_decl(ctx.fun_decl());
		} else {
			return visitVar_decl(ctx.var_decl());
		}
	}

	@Override
	public Variable_Declaration visitVar_decl(MiniCParser.Var_declContext ctx) {

		switch (ctx.getChild(2).getText()) {
		case ";":
			return new Variable_Declaration(
					visitType_spec(ctx.type_spec()), ctx.IDENT());
		case "=":
			return new Variable_Declaration_Assign(
					visitType_spec(ctx.type_spec()), ctx.IDENT(),
					ctx.LITERAL());
		case "[":
			return new Variable_Declaration_Assign(
					visitType_spec(ctx.type_spec()), ctx.IDENT(),
					ctx.LITERAL());
		default:
			return null;
		}
	}

	@Override
	public TypeSpecification visitType_spec(MiniCParser.Type_specContext ctx) {

		Type type = ctx.getChild(0).getText().equals("void") ? Type.VOID
				: Type.INT;

		return new TypeSpecification(type);
	}

	@Override
	public Function_Declaration visitFun_decl(MiniCParser.Fun_declContext ctx) {

		return new Function_Declaration(
				visitType_spec(ctx.type_spec()), ctx.IDENT(),
				visitParams(ctx.params()),
				visitCompound_stmt(ctx.compound_stmt()));
	}

	@Override
	public Parameters visitParams(MiniCParser.ParamsContext ctx) {
		
		if(ctx.getChildCount() == 0){
			return new Parameters();
		}
		else if(ctx.getChild(0).getText().equals("void")){
			return new Parameters(new TypeSpecification(Type.VOID));
		}
		else{
			List<Parameter> params = new ArrayList<Parameter>();
			
			for(int i = 0; i < ctx.param().size(); i++){
				params.add(visitParam(ctx.param(i)));
			}
			return new Parameters(params);
		}
	}

	@Override
	public Parameter visitParam(MiniCParser.ParamContext ctx) {
		if (ctx.getChildCount() == 4 && ctx.getChild(2).getText().equals("["))
			return new ArrayParameter(
					visitType_spec(ctx.type_spec()), ctx.IDENT());
		else
			return new Parameter(visitType_spec(ctx.type_spec()),
					ctx.IDENT());
	}

	@Override
	public Statement visitStmt(MiniCParser.StmtContext ctx) {

		if (ctx.getChild(0) instanceof MiniCParser.Expr_stmtContext) {
			return visitExpr_stmt(ctx.expr_stmt());
		} else if (ctx.getChild(0) instanceof MiniCParser.Compound_stmtContext) {
			return visitCompound_stmt(ctx.compound_stmt());
		} else if (ctx.getChild(0) instanceof MiniCParser.If_stmtContext) {
			return visitIf_stmt(ctx.if_stmt());
		} else if (ctx.getChild(0) instanceof MiniCParser.While_stmtContext) {
			return visitWhile_stmt(ctx.while_stmt());
		} else {
			return visitReturn_stmt(ctx.return_stmt());
		}
	}

	@Override
	public Expression_Statement visitExpr_stmt(MiniCParser.Expr_stmtContext ctx) {

		return new Expression_Statement(visitExpr(ctx.expr()));
	}

	@Override
	public While_Statement visitWhile_stmt(MiniCParser.While_stmtContext ctx) {

		return new While_Statement(ctx.WHILE(), visitExpr(ctx.expr()),
				visitStmt(ctx.stmt()));
	}

	@Override
	public Compound_Statement visitCompound_stmt(
			MiniCParser.Compound_stmtContext ctx) {
		List<Local_Declaration> localDecls = new ArrayList<Local_Declaration>();
		List<Statement> stmts = new ArrayList<Statement>();

		
		for (int i = 0; i < ctx.local_decl().size(); i++) {
			localDecls.add(visitLocal_decl(ctx.local_decl(i)));
		}
		for (int i = 0; i < ctx.stmt().size(); i++) {
			stmts.add(visitStmt(ctx.stmt(i)));
		}

		return new Compound_Statement(localDecls, stmts);
	}

	@Override
	public Local_Declaration visitLocal_decl(MiniCParser.Local_declContext ctx) {

		switch (ctx.getChild(2).getText()) {
		case ";":
			return new Local_Declaration(
					visitType_spec(ctx.type_spec()), ctx.IDENT());
		case "=":
			return new Local_Variable_Declaration_Assign(
					visitType_spec(ctx.type_spec()), ctx.IDENT(),
					ctx.LITERAL());
		case "[":
			return new Local_Variable_Declaration_Assign(
					visitType_spec(ctx.type_spec()), ctx.IDENT(),
					ctx.LITERAL());
		default:
			return null;
		}
	}

	@Override
	public If_Statement visitIf_stmt(MiniCParser.If_stmtContext ctx) {

		if (ctx.getChildCount() == 5) {
			return new If_Statement(ctx.IF(), visitExpr(ctx.expr()),
					visitStmt(ctx.stmt(0)));
		} else {
			return new If_Statement(ctx.IF(), visitExpr(ctx.expr()),
					visitStmt(ctx.stmt(0)), ctx.ELSE(),
					visitStmt(ctx.stmt(1)));
		}
	}

	@Override
	public Return_Statement visitReturn_stmt(MiniCParser.Return_stmtContext ctx) {

		if (ctx.getChildCount() == 2)
			return new Return_Statement(ctx.RETURN());
		else
			return new Return_Statement(ctx.RETURN(),
					visitExpr(ctx.expr()));
	}

	@Override
	public Expression visitExpr(MiniCParser.ExprContext ctx) {

		switch (ctx.getChildCount()) {
		case 1:
			return new TerminalExpression((TerminalNode) ctx.getChild(0));
		case 2:
			return new UnaryOpNode(ctx.op.getText(),
					visitExpr(ctx.expr(0)));
		case 3:
			if (isBinaryOperation(ctx)) {
				if (ctx.getChild(1).getText().equals("=")) {
					return new AssignNode(ctx.IDENT(),
							visitExpr(ctx.expr(0)));
				} else {
					return new BinaryOpNode(
							visitExpr(ctx.expr(0)),
							ctx.getChild(1).getText(),
							visitExpr(ctx.expr(1))
							);
				}
			} else {
				return new ParenExpression(visitExpr(ctx.expr(0)));
			}
		case 4:
			if (isFunctionExpr(ctx)) {
				return new FuncallNode(ctx.IDENT(), visitArgs(ctx.args()));
			} else {
				return new ArefNode(ctx.IDENT(), visitExpr(ctx.expr(0)));
			}
		case 6:
			return new ArefAssignNode(ctx.IDENT(), 
					visitExpr(ctx.expr(0)), 
					visitExpr(ctx.expr(1)));
		}

		return null;
	}

	@Override
	public Arguments visitArgs(MiniCParser.ArgsContext ctx) {

		if(ctx.getChild(0).equals("")){
			return new Arguments();
		}
		else{
			List<Expression> args = new ArrayList<Expression>();

			for(int i = 0; i < ctx.expr().size(); i++)
				args.add(visitExpr(ctx.expr(i)));
			return new Arguments(args);
		}
	}

}
