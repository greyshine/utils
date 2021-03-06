package de.greyshine.utils;

import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Helper to evaluate <tt>public static void main(String[] args)</tt>-Args.
 * 
 * How it works:<br/>
 * <ol>
 * <li>Create a static instance of {@link CommandLineParser} and configure it via flow api.</li>
 * <li>the static instance is able to create {@link Args} instances by parsing the {@link String[]} main args.</li>
 * </ol>
 * Or even simplier:
 * <code>Args theArgs = CommandLineParse.args( String[] args );</code>
 * 
 * 
 */
public class CommandLineParser {

	enum Stream {
		/**
		 * DEV1
		 */
		OUT,
		/**
		 * DEV2
		 */
		ERR;
		
		public PrintStream getStream() {
			
			switch ( this ) {
			case OUT:
				return System.out;
			case ERR:
				return System.err;
			default:
				throw new IllegalStateException("must not occur");
			}
		};
	}
	
	private List<Option> options = new ArrayList<>();
	private List<SimpleArg> simpleArgs = new ArrayList<>(0);

	private String headerText;
	private String usageText;
	private String footerText;

	/**
	 * Fastes, easiest and simpliest way to gracefully acces the args.
	 * @param inArgs
	 * @return
	 */
	public static Args args(String[] inArgs) {
		return new CommandLineParser().parse( inArgs );
	}
	
	/**
	 * A simple arg. Is not prefixed by any amount of hyphens.<br/>
	 * Arguments passed at last with a command.<br/>
	 * Any 'simple arg' in between of options will be ignored.<br/>
	 * The last of the simple arguments may be set <code>multi<code>. This allows an amount of arguments which could be helpful when e.g. denoting files.
	 */
	public class SimpleArg {
		
		private String name;
		private boolean isOptional = false;
		private boolean isMulti = false;
		private String description;
		
		private SimpleArg(String inName) {
			if ( inName == null ) throw new IllegalArgumentException("name must not be blank");
			name = Utils.trimToNull( inName );
			simpleArgs.add( this );
		}
		
		public SimpleArg multi() {
			for (SimpleArg simpleArg : simpleArgs) {
				if ( simpleArg.isMulti ) {
					throw new IllegalStateException("there has already bee a parameter with option multi: "+ simpleArg.name);
				}
			}
			
			this.isMulti = true;
			return this;
		}
		
		public SimpleArg optional() {
			isOptional = true;
			return this;
		}
		
		public SimpleArg description(String inDescription) {
			this.description = inDescription;
			return this;
		}
		
		public CommandLineParser done() {
			return CommandLineParser.this;
		}
		
	}
	
	public boolean isDeclaredOption(String inOption) {
		
		for( Option o : options ) {
			if ( o.option.equals( inOption ) ) {
				return true;
			}
		}
		
		return false;
	}

	public boolean isDeclaredLongoption(String inOption) {
		
		for( Option o : options ) {
			if ( o.longoption != null && (o.longoption.equals( inOption ) || o.longoption.equals( inOption ) ) ) {
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Option is an argument out of the {@link String}-array <code>args[]</code>.<br/>
	 * The argument is prefixed with a hyphen in the short version.<br/>
	 * The long version is prefixed with two hyphens. Both of them are treated equally.<br/>
	 * Depending on setting up the {@link CommandLineParser} an option may have a parameter which when registered must exist.<br/>
	 * Me, I called it parameter beacaus calling it arg of an arg sounds confusing. 
	 */
	public class Option {

		private String option;
		private String longoption;

		private String parameterName;
		private Pattern parameterPattern;
		
		private String description;

		/**
		 * Must this option be existing?
		 */
		private boolean isOptional = false;

		private Option(String inOption) {
			
			option = inOption.trim();
			
			if ( isDeclaredOption(inOption) || isDeclaredLongoption(inOption) ) {
				throw new IllegalArgumentException("option already declared: "+ inOption);
			}
			
			options.add( this );
		}

		public Option optional() {
			return optional(true);
		}

		public Option optional(boolean isOptional) {
			this.isOptional = isOptional;
			return this;
		}

		public Option longoption(String inLongoption) {

			final String theLongoption = inLongoption == null ? "" : inLongoption.trim();
			if (theLongoption.isEmpty()) {
				return this;
			}

			final Option that = this;
			options.stream().forEach((o) -> {
				if (o != that && o.longoption.equals(inLongoption)) {
					throw new IllegalArgumentException("longoption '" + inLongoption + "'already used: " + o);
				}
			});

			longoption = theLongoption;
			return this;
		}

		public Option parameter(String inArgParameter) {
			if ( inArgParameter == null || inArgParameter.trim().isEmpty()) { throw new IllegalArgumentException("arg does not have a name"); }
			this.parameterName = inArgParameter.trim();
			return this;
		}

		public boolean isOptionParameterized() {
			return parameterName != null;
		}

		public Option description(String inDescription) {
			this.description = inDescription;
			return this;
		}

		@Override
		public String toString() {
			return "Option -" + option;
		}

		public CommandLineParser done() {
			return CommandLineParser.this;
		}

		public Option regex(String inRegex) {
			this.parameterPattern =  inRegex==null? null : Pattern.compile( inRegex );  
			return this;
		}
	}
	
	public SimpleArg simpleArg(String inName) {
		
		if ( Utils.isBlank( inName ) ) { return null; }
		
		return new SimpleArg(inName);
	}

	public Option option(String inOption) {
		return option( inOption, null );
	}
	
	public Option option(String inOption, String inLongoption) {
		
		if (inOption == null || (inOption = inOption.trim()).isEmpty()) {
			return null;
		}
		
		for (Option o : options) {
			
			if (o.option.equals(inOption)) {
				return o;
			}
		}
		
		return new Option(inOption).longoption(inLongoption);
	}

	public CommandLineParser usageText(String inUsageText) {
		usageText = "usage: " + (inUsageText == null ? "" : inUsageText);
		return this;
	}
	
	private String createUsageText(String inCommand) {
		
		String theUsageLine = inCommand != null ? inCommand : "";
		for (Option anOption : options) {
			
			theUsageLine +=" ";
			if ( anOption.isOptional ) {
				theUsageLine += "[";
			}
			theUsageLine += "-"+ anOption.option;
			
			if ( anOption.parameterName != null ) {
				theUsageLine += " <"+ anOption.parameterName +">";
			}
			
			if ( anOption.isOptional ) {
				theUsageLine += "]";
			}
		}
		
		for( SimpleArg sa : simpleArgs ) {
			theUsageLine += " ";
			theUsageLine += "<"+sa.name+">";
			if ( sa.isMulti ) {
				theUsageLine+="...";
			}
		}
		
		String optionsBlock = "";

		final int theLengthOptions = options.stream().mapToInt((o) -> {

			// "-option"
			int w = 1 + o.option.length();
			// ", --longoption"
			w += (o.longoption == null ? 0 : 4 + o.longoption.length());
			
			w += ( !o.isOptionParameterized() ? 0 : o.parameterName.length() + 3);
			
			return w;

		}).max().orElse(0);

		int theLengthSimpleArgs = simpleArgs.stream().mapToInt((sa) -> {
			
			int len = sa.name.length();
			len += 2; // +2 because of "<name>"
			len += ( sa.isMulti ? 3 : 0 ); // beacause "..."
			return len;
			
		}).max().orElse(0);
		
		final int theLength = 2+Math.max( theLengthOptions , theLengthSimpleArgs);
		
		for (Option anOption : options) {
			
			String optionSubBlock = "-"+anOption.option;
			if (anOption.longoption != null) {
				optionSubBlock += ", --" + anOption.longoption;
			}
			
			if ( anOption.isOptionParameterized() ) {
				optionSubBlock += " <"+ anOption.parameterName +">";
			}

			while (optionSubBlock.length() < theLength) {
				optionSubBlock += " ";
			}

			boolean isFirstLine = true;
			for (String dLine : (anOption.description == null ? "" : anOption.description).split("\\n", -1)) {

				if (isFirstLine) {

					isFirstLine = false;

				} else {

					for (int i = 0; i < theLength; i++) {
						dLine = " " + dLine;
					}

					dLine = "\n" + dLine;
				}

				optionSubBlock += dLine;
			}

			optionsBlock += "\n" + optionSubBlock;
		}
		
		if ( !simpleArgs.isEmpty() ) {
			optionsBlock +="\n";
		}
		
		for( SimpleArg sa : simpleArgs ) {
			
			String optionSubBlock = "<"+ sa.name +">";
			
			if (sa.isMulti) {
				optionSubBlock += "...";
			}
			
			while( optionSubBlock.length() < theLength ) {
				optionSubBlock += " ";
			}
			
			boolean isFirstLine = true;
			for (String dLine : (sa.description == null ? "" : sa.description).split("\\n", -1)) {

				if (isFirstLine) {

					isFirstLine = false;

				} else {

					for (int i = 0; i < theLength; i++) {
						dLine = " " + dLine;
					}

					dLine = "\n" + dLine;
				}

				optionSubBlock += dLine;
			}
			
			optionsBlock += "\n" + optionSubBlock;
			
		}
		
		return theUsageLine + System.lineSeparator() + optionsBlock;
	}

	/**
	 * 
	 * @param inCommand the command to be executed. Actually the text to display how to invoke the command.
	 * @return
	 */
	public CommandLineParser generateUsageText(String inCommand) {

		final String theUsageText = createUsageText(inCommand);
		
		return usageText( theUsageText );
	}

	public CommandLineParser headerText(String inHeaderText) {
		headerText = inHeaderText;
		return this;
	}

	public CommandLineParser footerText(String inFooterText) {
		footerText = inFooterText;
		return this;
	}

	public String getHelp() {
		return getHelp(null);
	}
		public String getHelp(String inCustomMessage) {

		inCustomMessage = Utils.trimToNull( inCustomMessage );

		final StringBuilder sb = new StringBuilder();
		
		if ( inCustomMessage != null ) {
			sb.append( inCustomMessage ).append( "\n\n" );
		}

		if (headerText != null) {
			sb.append(headerText);
			sb.append(System.lineSeparator());
		}

		String theUsageText = usageText != null ? usageText : createUsageText(null);
		
		sb.append(theUsageText);
		
		if (footerText != null) {
			sb.append(System.lineSeparator());
			sb.append(footerText);
		}

		return sb.toString().trim();
	}

	public void printHelp(String inCustomMessage, Stream inStream) {
		
		inStream = inStream != null ? inStream : Stream.OUT;
		
		final String theHelpText = getHelp(inCustomMessage);
		
		inStream.getStream().println( theHelpText );
	}
	
	public void printHelp() {
		
		printHelp( null, null );
	}

	public Args parse(String[] inArgs) {
		return new Args(inArgs);
	}

	public Option lookupOption(String inOption) {
		for (Option option : options) {
			if ( option.option.equals( inOption ) ) {
				return option;
			} else if ( option.longoption != null && option.longoption.equals( inOption ) ) {
				return option;
			}
		}
		return null;
	}
	
	public Option lookupLongoption(String inLongOption) {
		for (Option option : options) {
			if ( option.longoption != null && option.longoption.equals( inLongOption ) ) {
				return option;
			}
		}
		return null;
	}
	
	public class Args {

		/**
		 * No index Will ever contain a <code>null</code> value.
		 */
		private final String[] args;
		
		public Args(String[] args) {

			this.args = args == null ? new String[0] : args;

			for (int i = 0; i < args.length; i++) {
				this.args[i] = this.args[i] == null ? "" : this.args[i];
			}
		}
		
		public boolean isExistingArgAtIndex(int inIndex) {
			return inIndex >= 0 && inIndex < args.length;
		}
		
		public int getArgsCount() {
			return args.length;
		}
		
		public List<String> getArgs() {
			
			final List<String> theArgs = new ArrayList<>(0);
			
			for( String a : args ) {
				theArgs.add( a );
			}
			
			return theArgs;
		}
		
		@Override
		public String toString() {
			final StringBuilder sb = new StringBuilder();
			Arrays.stream( args ).forEach( (anArg)->{ sb.append( anArg ).append( ' ' ); } );
			return "Args ["+ sb.toString().trim() +"]";
		}

		/**
		 * 
		 * @return values after the last Option (and its parameter if existing)
		 */
		public List<String> getSimpleArgs() {

			// take last optional argument, fetch its index check if there might be an optional arg for that option
			// then take the last arguments as method result
			
			final List<String> vs = new ArrayList<>();
			
			for( int i = args.length-1 ; i >= 0 ; i-- ) {
				
				final String theArg = args[i];
				
				final Option theOption = !theArg.startsWith( "-" ) ? null : getOption( theArg );
				
				if ( theOption == null ) {
					
					vs.add( unwrap( theArg ) );
					continue;
				
				} else if ( theOption.isOptionParameterized() && !vs.isEmpty()  ) {
					
					vs.remove( vs.size()-1 );
				}
				
				break;
			}
			
			Collections.reverse( vs );
			
			return vs;
		}

		public List<File> getSimpleArgsAsFiles() {

			final List<File> vs = new ArrayList<>();

			getSimpleArgs().stream().filter(Utils::isNotBlank).map(File::new).forEach(vs::add);

			return vs;
		}
		
		public boolean isSimpleArgNotBlank(int inIndex) {
			final String theValue = getSimpleArg( inIndex );
			return theValue != null && !theValue.trim().isEmpty();
		}
		
		public String getSimpleArg(int inIndex) {
			return Utils.getIndexedValueSafe( getSimpleArgs(), inIndex, null);			
		}
		
		public File getSimpleArgAsFile(int inIndex) {
			final String theValue = getSimpleArg(inIndex);
			return Utils.isBlank( theValue ) ? null : new File( theValue );
		}

		public int getOptionIndex(String inOption) {
			return getOptionIndex( lookupOption( inOption ) );
		}

		private int getOptionIndex(Option inOption) {
			
			if ( inOption == null ) { return -1; }
			
			final String o = "-" + inOption.option;
			final String lo = inOption.longoption == null ? null : "--" + inOption.longoption;
			
			for (int i = 0; i < args.length; i++) {
				if (o.equals(args[i])) {
					return i;
				} else if ( lo != null && lo.equals( args[i] ) ) {
					return i;
				}
			}
			
			return -1;
		}

		/**
		 * Is the given option one of the elements of <code>args[]</code>.<br/>
		 * Do not prefix with any hyphens!
		 * @param inOption
		 * @return is existing option prefixed with <tt>-</tt>
		 */
		public boolean isOption(String inOption) {
			return getOptionIndex(inOption) > -1;
		}
		
		public String getArg(int inIndex) {
			return getArg(inIndex, true, null);
		}
		
		public String getArg(int inIndex, boolean isTrim, String inDefaultIfBlank) {

			inIndex = inIndex >= 0 ? inIndex : inIndex + args.length;

			if ( inIndex < 0 || inIndex >= args.length ) { return inDefaultIfBlank; }
			
			String theArg = null;

			try {

				theArg = args[inIndex];
				theArg = !isTrim ? theArg : theArg.trim();
				theArg = theArg.isEmpty() ? null : theArg;
				theArg = unwrap( theArg );
				
			} catch (Exception e) {
				// swallow
			}

			return theArg != null ? theArg : inDefaultIfBlank;
		}
		
		public boolean isOption(int inIndexPos) {
			return getOption(inIndexPos) != null;
		}
		
		public Option getOption(int inIndexPos) {
			
			final String theArg = Utils.getIndexedValueSafe(args, inIndexPos, null);
			if ( theArg == null ) { return null; }
			
			for (Option o : options) {
				if ( theArg.equals( "-"+o.option ) || theArg.equals( "--"+o.longoption ) ) {
					return o;
				}
			}
			
			return null;
		}
		
		public Option getOption(String inOption) {
			
			inOption = removeHyphens(inOption);
			
			for (Option o : options) {
				
				if ( o.option.equals( inOption ) ) {
					
					return o;
				
				} else if ( o.longoption != null && o.longoption.equals( inOption ) ) {
					
					return o;
				}
			}
			
			return null;
		}

		public String getOptionParameter(String inOption) {
			return getOptionParameter( inOption, null );
		}
		
		public String getOptionParameter(String inOption, String inDefault) {
			
			final Option theOption = lookupOption( inOption );
			if ( !theOption.isOptionParameterized()) { return inDefault; }
			
			final int theIndex = getOptionIndex( theOption );
			
			if ( theIndex == -1 ) { return inDefault; }
			
			final String theResult = getArg( theIndex+1 );
			
			return theResult == null ? inDefault : theResult;
		}
		
		public Integer getOptionParameterAsInt(String inOption, Integer inDefault) {
			return Utils.parseInteger(getOptionParameter(inOption), inDefault);
		}
		
		public File getOptionParameterAsFile(String inOption) {
			return getOptionParameterAsFile(inOption, null);
		}
		
		public File getOptionParameterAsFile(String inOption, File inDefault) {
			return Utils.toFile( getOptionParameter( inOption ), true, true );
		}
		
		/**
		 * Does it contain the flag <tt>v</tt>
		 * @return
		 */
		public boolean isVerbose() {
			return isOption( "v" );
		}
		
		/**
		 * Does it contain the flag <tt>q</tt>
		 * @return
		 */
		public boolean isQuiet() {
			return isOption( "q" );
		}

		/**
		 * @return whether the args are empty
		 */
		public boolean isEmpty() {
			return args.length < 1;
		}
	}
	
	private static String removeHyphens(String inOption) {
		
		if ( inOption == null || !inOption.startsWith( "-" )) { return inOption; }
		
		return inOption.substring( inOption.startsWith( "--" ) ? 2 : 1 ); 
	}
	
	private static String unwrap(String inArg) {
		
		if ( inArg == null ) { return inArg; }
		
		final String unwrapped = Utils.unwrap( inArg, '\"' ); 
		
		return !inArg.equals( unwrapped ) ? unwrapped : Utils.unwrap(inArg, '\'');
	}

	/**
	 * Adds the option <code>v</code> and <code>verbose</code> for being more chatty.
	 * @return
	 */
	public CommandLineParser optionVerbose() {
		return option( "v", "verbose" ).optional().description( "be more chatty" ).done();
	}
	
	/**
	 * Adds the option <code>q</code> and <code>quiet</code> for being quiet as possible with the output.
	 * @return
	 */
	public CommandLineParser optionQuiet() {
		return option( "q", "quiet" ).optional().description( "be as quiet as possible" ).done();
	}

	public CommandLineParser optionHelp() {
		return option("h", "help").optional().description("show the help information").done();
	}
	
}
