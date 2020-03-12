package ru.windcorp.jputil.cmd.parsers;

public class SyntaxFormatters {
	
	private SyntaxFormatters() {}
	
	public static final SyntaxFormatter PLAIN = new SyntaxFormatter() {
		
		@Override
		public void appendType(StringBuilder sb, String type) {
			sb.append(type);
		}
		
		@Override
		public void appendTrailing(StringBuilder sb) {
			sb.append("...");
		}
		
		@Override
		public void appendStructureChar(StringBuilder sb, char c) {
			sb.append(c);
		}
		
		@Override
		public void appendId(StringBuilder sb, String id) {
			sb.append(id);
		}
		
		@Override
		public void appendLiteral(StringBuilder sb, String contents) {
			sb.append(contents);
		}
	};

}
