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
          <#if funder.name == "España. Ministerio de Ciencia y Tecnología" || funder.name == "Ministerio de Ciencia y Tecnología" || funder.name == "Ministry of Science and Technology" || funder.name == "Spanish Ministry of Science and Technology" || funder.name == "MICYT">
            <funderIdentifier funderIdentifierType="Crossref Funder ID">http://dx.doi.org/10.13039/501100006280</funderIdentifier>
          <#elseif funder.name == "Spanish Ministry of Science and Innovation" || funder.name == "Ministerio de Ciencia e Innovación" || funder.name == "Ministry of Science and Innovation" || funder.name == "MICINN">
            <funderIdentifier funderIdentifierType="Crossref Funder ID">http://dx.doi.org/10.13039/501100004837</funderIdentifier>
          <#elseif funder.name == "Spanish Ministry of Economy and Competitiveness" || funder.name == "Gobierno de España. Ministerio de Economía y Competitividad" || funder.name == "Ministerio de Economía y Competitividad" || funder.name == "Ministry of Economy and Competitiveness" || funder.name == "MINECO">
            <funderIdentifier funderIdentifierType="Crossref Funder ID">http://dx.doi.org/10.13039/501100003329</funderIdentifier>
          <#elseif funder.name == "Consejería de Educación, Juventud y Deporte, Comunidad de Madrid" || funder.name == "Consejería de Educación, Juventud y Deporte de la Comunidad de Madrid" || funder.name == "Ministry of Education, Youth and Sports, Government of Madrid">
            <funderIdentifier funderIdentifierType="Crossref Funder ID">http://dx.doi.org/10.13039/501100008433</funderIdentifier>
          <#elseif funder.name == "Agencia Antidroga de la Comunidad de Madrid" || funder.name == "Anti-Drug Agency of the Community of Madrid">
            <funderIdentifier funderIdentifierType="Crossref Funder ID">http://dx.doi.org/10.13039/501100006542</funderIdentifier>
          <#elseif funder.name == "Oficina Regional de Coordinación de Salud Mental, Comunidad de Madrid" || funder.name == "Oficina Regional de Coordinación de Salud Mental">
            <funderIdentifier funderIdentifierType="Crossref Funder ID">http://dx.doi.org/10.13039/501100006543</funderIdentifier>
          <#elseif funder.name == "Consejería de Sanidad, Comunidad de Madrid">
            <funderIdentifier funderIdentifierType="Crossref Funder ID">http://dx.doi.org/10.13039/501100006541</funderIdentifier>
	  <#elseif funder.name == "Instituto de Salud Carlos III" || funder.name == "Institute of Health Carlos III" || funder.name == "Carlos III Health Institute" || funder.name == "ISCIII">
            <funderIdentifier funderIdentifierType="Crossref Funder ID">http://dx.doi.org/10.13039/501100004587</funderIdentifier>
          <#elseif funder.name == "ERDF" || funder.name == "Fondo Europeo de Desarrollo Regional" || funder.name == "European Regional Development Fund">
            <funderIdentifier funderIdentifierType="Crossref Funder ID">http://dx.doi.org/10.13039/501100008530</funderIdentifier>
          <#elseif funder.name == "EPSRC" || funder.name == "Engineering and Physical Sciences Research Council" >
            <funderIdentifier funderIdentifierType="Crossref Funder ID">http://dx.doi.org/10.13039/501100000266</funderIdentifier>
          <#elseif funder.name == "ERC" || funder.name == "European Research Council" >
            <funderIdentifier funderIdentifierType="Crossref Funder ID">http://dx.doi.org/10.13039/501100000781</funderIdentifier>
          </#if>
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
        <rights rightsURI="info:eu-repo/semantics/openAccess" />
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
