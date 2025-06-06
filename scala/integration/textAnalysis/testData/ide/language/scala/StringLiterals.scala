class Main {
  "It is <GRAMMAR_ERROR descr="Use 'a' before a consonant sound">an </GRAMMAR_ERROR>friend of human";
  "It is <TYPO descr="Typo: In word 'frend'">frend</TYPO> of human";
  "It <GRAMMAR_ERROR descr="Subject-verb agreement seems to be violated">are</GRAMMAR_ERROR> working for <GRAMMAR_ERROR descr="Use 'many' with plural nouns like 'warnings'">much</GRAMMAR_ERROR> warnings";
  "It is ${1} friend";
  "It is <GRAMMAR_ERROR descr="Missing article?">friend</GRAMMAR_ERROR>. But I have a ${1} here";

  """
    |Lorem ipsum dolor sit amet,
    |<TYPO descr="Typo: In word 'onsectetur'">onsectetur</TYPO>...
    |""".stripMargin;

  """
    |Lorem ipsum dolor sit amet,
    |<TYPO descr="Typo: In word 'onsectetur'">onsectetur</TYPO>...
    |""".stripMargin;

  System.out.println("It is <GRAMMAR_ERROR descr="Use 'a' before a consonant sound">an </GRAMMAR_ERROR>friend of human");
  System.out.println("It is <TYPO descr="Typo: In word 'frend'">frend</TYPO> of human");
  System.out.println("It <GRAMMAR_ERROR descr="Subject-verb agreement seems to be violated">are</GRAMMAR_ERROR> working for <GRAMMAR_ERROR descr="Use 'many' with plural nouns like 'warnings'">much</GRAMMAR_ERROR> warnings");
  System.out.println("It is ${1} friend");
  System.out.println("It is <GRAMMAR_ERROR descr="Missing article?">friend</GRAMMAR_ERROR>. But I have a ${1} here");
  System.out.println("The path <GRAMMAR_ERROR descr="Too many punctuation marks">is ..</GRAMMAR_ERROR>/data/test.avi");

  "(<GRAMMAR_ERROR descr="This word is usually spelled with a hyphen">cherry picked</GRAMMAR_ERROR> from "; // hard-coding the string git outputs
  "I'd like to <GRAMMAR_ERROR descr="This word is usually spelled with a hyphen">cherry pick</GRAMMAR_ERROR> this";
}
