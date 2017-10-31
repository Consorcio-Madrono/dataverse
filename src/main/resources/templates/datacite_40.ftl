<?xml version="1.0" encoding="UTF-8"?>
<resource xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns="http://datacite.org/schema/kernel-4" xsi:schemaLocation="http://datacite.org/schema/kernel-4 http://schema.datacite.org/meta/kernel-4/metadata.xsd">
    <identifier identifierType="${identifierType}">${identifier}</identifier>
    <creators>
    <#list authors as author>
        <creator>
            <creatorName>${author.name.displayValue}</creatorName>
            <#if author.idValue?has_content>
            <nameIdentifier nameIdentifierScheme="${author.idType}">${author.idValue}</nameIdentifier>
            </#if>
            <#if author.affiliation??>
              <#if author.affiliation.displayValue?has_content>
                <affiliation>${author.affiliation.displayValue}</affiliation>
              </#if>
            </#if>
        </creator>
    </#list>
    </creators>
    <titles>
        <title <#if language??>xml:lang="${language}"</#if>>${title}</title>
        <#if subtitle??>
        <title <#if language??>xml:lang="${language}" </#if>titleType="Subtitle">${subtitle}</title>
        </#if>
        <#if alternativeTitle??>
        <title <#if language??>xml:lang="${language}" </#if>titleType="AlternativeTitle">${alternativeTitle}</title>
        </#if>
    </titles>
    <#if publisher??>
    <publisher>${publisher}</publisher>
    </#if>
    <#if publisherYear??>
    <publicationYear>${publisherYear}</publicationYear>
    </#if>
    <#if subjects??>
    <subjects>
        <#list subjects as subject>
        <#if subject.value??>
        <subject<#if subject.uri??> schemeURI="${subject.uri}"</#if><#if subject.scheme??> subjectScheme="${subject.scheme}"</#if>>${subject.value}</subject>
        </#if>
        </#list>
    </subjects>
    </#if>
    <#if funders?has_content>
    <fundingReferences>
        <#list funders as funder>
        <fundingReference>
            <funderName>${funder.name}</funderName>
            <awardNumber>${funder.awardNumber}</awardNumber>
        </fundingReference>
        </#list>
    </fundingReferences>
    </#if>
    <dates>
        <#if dateOfDeposit??>
        <date dateType="Submitted">${dateOfDeposit}</date>   
        </#if>
        <#if dateOfCollection??>
        <date dateType="Collected">${dateOfCollection.start}</date>
        </#if>
        <#if lastUpdated??>
        <date dateType="Updated">${lastUpdated}</date>
        </#if>
    </dates>
    <resourceType resourceTypeGeneral="Dataset"><#if resourceType??>${resourceType}</#if></resourceType>
    <#if publications?has_content>
    <relatedIdentifiers>
        <#list publications as pub>
	  <#if pub.idNumber??>
	    <#if pub.idType == "DOI" || pub.idType == "arXiv" || pub.idType == "bibcode"> 
              <relatedIdentifier relationType="IsCitedBy" relatedIdentifierType="${pub.idType}">
                ${pub.idNumber}
              </relatedIdentifier>                
	    <#elseif pub.idType == "handle"> 
              <relatedIdentifier relationType="IsCitedBy" relatedIdentifierType="Handle">
                ${pub.idNumber}
              </relatedIdentifier>                
	    <#elseif pub.idType == "ark" || pub.idType == "ean13" || pub.idType == "eissn" || pub.idType == "isbn" || pub.idType == "issn" || pub.idType == "istc" || pub.idType == "lissn" || pub.idType == "lsid" || pub.idType == "pmid" || pub.idType == "purl" || pub.idType == "upc" || pub.idType == "url" || pub.idType == "urn"> 
              <relatedIdentifier relationType="IsCitedBy" relatedIdentifierType="${pub.idType?upper_case}">
                ${pub.idNumber}
              </relatedIdentifier>                
            </#if>
          </#if>
        </#list>
    </relatedIdentifiers>
    </#if>
    <#if rights??>
    <rightsList>
        <rights<#if rightsUri??> rightsURI="${rightsUri}"</#if>>${rights}</rights>
        <rights rightsURI=”info:eu-repo/semantics/openAccess” />
    </rightsList>
    </#if>
    <#if description??>
    <descriptions>
        <description <#if language??>xml:lang="${language}" </#if>descriptionType="Abstract">${description}</description>
    </descriptions>
    </#if>
    <#if contributors?has_content>
    <contributors>
        <#list contributors as contrib>
	  <#if contrib.composedName??>
            <contributor contributorType="${contrib.role}">
              <contributorName>${contrib.composedName}</contributorName>
            </contributor>        
          </#if>
        </#list>
    </contributors>
    </#if>
    <#if language??>
    <language>${language}</language>    
    </#if>
    <#if version??>
    <version>${version}</version>
    </#if>
</resource>
