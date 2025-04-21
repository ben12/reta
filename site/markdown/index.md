## About

RETA (Requirement Engineering Traceability Analysis) can read any source of documents or code, extracts the requirements and requirement references, and analyses them for generate the traceability matrix.

## Installation

Download then install [the last release](https://github.com/ben12/reta/releases/latest).

## Plugins

### TIKA plugin

TIKA plugin uses [Apache Tika](https://tika.apache.org/) to read any document (doc, xls, pdf, ...) and extracts requirements and requirement references from it.

#### Configuration

Requirements and references are extracted from documents using regular expression. You can find documentation on the web and for example here : [Pattern JAVADOC](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/regex/Pattern.html#construct)

So, if your requirements are formatted like this in your document :

> REQ_RETA_SRS_*NNNN*_*v* - *summary*
> 
> *content*
> 
> REQ_END

Where "NNNN" is the requirement number, "v" the version, "summary" the summary description and "content" the detailed description.

Then, regular expression to match requirement start may be :  
   `^[ \t]*REQ_((RETA_SRS_\d+)_(\w+)[\s-]+(.*?))$`  
And regular expression to match requirement end may be :  
   `^[ \t]*REQ_END[ \t]*$`

Bracket will capture part of text which will be used, for example, for identify the requirement reference using "Id" attribute ([Pattern JAVADOC](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/regex/Pattern.html#cg)).  
For done this, you must identify each capture group using "Index of regular expression groups in requirement starts" table and "Index of regular expression groups in references" table.  
In this sample:
 - "Text" attribute must be set to *1* for capture "RETA_SRS_*NNNN*_*v* - *summary*",
 - "Id" attribute must be set to *2* for capture "RETA_SRS_*NNNN*",
 - "Version" attribute must be set to *3* for capture "*v*",
 - "Summary" attribute must be set to *4* for capture "*summary*",

Text and Id attributes are required to build the matrix.

## Export

RETA allows you to export the analysis result to an Excel file.

## Alternatives to RETA

- Reqtify (https://www.3ds.com/products/catia/reqtify)