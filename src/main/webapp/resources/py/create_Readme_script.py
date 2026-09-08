#!/usr/bin/env python3
# coding: utf-8

# # Script to create a Readme file for a dataset in Dataverse
# ### OBSERVATION:
# This script is available in the following GitHub repository: <a href='https://github.com/CSUC/RDR-scripts/tree/main/related_publication_check' target='_blank'>RDR-scripts</a>. </p> If you have questions or doubts about the code, please contact rdr-contacte@csuc.cat.
# ### SCRIPT OBJECTIVE:
# The main objective of this script is to automatically create the README file for a dataset.
# 

import os
import subprocess
import sys
from lxml.html import fromstring # CONSORCIO. Remove html code from description

# Function to install required packages
def install_packages():
    """
    Function to install or update necessary Python packages.
    """
    # Upgrade pip first
    subprocess.check_call([sys.executable, "-m", "pip", "install", "--upgrade", "pip", "-q"])

    # Install the required libraries
    subprocess.check_call([sys.executable, "-m", "pip", "install", "pyDataverse", "-q"])

    print("Libraries have been downloaded or updated.")

# Install libraries if they are not installed already
try:
    import pyDataverse
except ImportError:
    print("Installing libraries...")
    install_packages()

# Proceed with the rest of the code
# Get the user token
token = sys.argv[1];
print ("Token: " + token);

# Get the doi or identifier
doi = sys.argv[2];
print ("doi: " + doi);

# Get the directory to put the readme
dest_dir = sys.argv[3];
print ("dest_dir: " + dest_dir);

# The base URL is always fixed, no need to ask the user
base_url = sys.argv[4];
print ("base_url: " + base_url);

# The identifier without the doi / handle part
identifier = sys.argv[5];
print ("identifier: " + identifier);

# The path to the properties file
properties_path = sys.argv[6];
print ("properties_path: " + properties_path);

# The lang of the readme file
language = sys.argv[7];
print ("language: " + language);

# You can now initialize the Dataverse API using the provided details
from pyDataverse.api import NativeApi

# Initialize the Dataverse API
native_api = NativeApi(base_url, token)

# Further operations like searching or getting metadata can be done below using the API

def translate (properties, properties_back, text, capitalize, upper):
    """
    Function that get a string in the selected lenguage from a given key.

    Parameters:
    - properties: The properties file with the translation in the selected language.
    - properties_back: The original properties file with the English translation.

    Returns:
    - tr_text: The string 
    """
    item= properties.get(text)
    if item is None:
        item= properties_back.get(text)
    if item is None:
        tr_text= text
    else:
        tr_text= item.data

    if capitalize:
        tr_text= tr_text.capitalize()
    if upper:
        tr_text= tr_text.upper()

    return tr_text
        

def extract_value(data_dict):
    """
    Function to extract all keys and values from a JSON metadata dictionary.

    Parameters:
    - data_dict: dict. JSON metadata dictionary.

    Returns:
    - type_names: list. List of type names extracted from the metadata.
    - values: list. List of values extracted from the metadata.
    """
    if isinstance(data_dict, dict):
        type_names = []
        values = []
        for key, value in data_dict.items():
            if key == 'typeName' and 'value' in data_dict:
                if isinstance(data_dict['value'], list):
                    for v in data_dict['value']:
                        type_names.append(data_dict['typeName'])
                        values.append(v)
                else:
                    type_names.append(data_dict['typeName'])
                    values.append(data_dict['value'])
            elif isinstance(value, dict) and 'typeName' in value and 'value' in value:
                type_names.append(value['typeName'])
                values.append(value['value'])
            elif isinstance(value, str) and key == 'typeName':
                type_names.append(value)
                values.append(value)
            else:
                extracted_type_names, extracted_values = extract_value(value)
                type_names += extracted_type_names
                values += extracted_values
        return type_names, values
    elif isinstance(data_dict, list):
        type_names = []
        values = []
        for item in data_dict:
            extracted_type_names, extracted_values = extract_value(item)
            type_names += extracted_type_names
            values += extracted_values
        return type_names, values
    else:
        return [], []
def exportmetadata(base_url, token, doi,
                   citation_keys, citation_values,
                   geo_keys, geo_values,
                   social_keys, social_values,
                   astronomy_keys, astronomy_values,
                   biomedical_keys, biomedical_values,
                   journal_keys, journal_values,
                   computationalworkflow_keys, computationalworkflow_values,
                   LocalContextsCVoc_keys,LocalContextsCVoc_values,
                   darwincore_keys, darwincore_values):
    """
    Function to export metadata from a dataset identified by its DOI.

    Parameters:
    - base_url: str. Base URL of the Dataverse instance.
    - token: str. API token for authentication.
    - doi: str. DOI of the dataset.
    - citation_keys: list. List to store citation metadata keys.
    - citation_values: list. List to store citation metadata values.
    - geo_keys: list. List to store geospatial metadata keys.
    - geo_values: list. List to store geospatial metadata values.
    - social_keys: list. List to store social science metadata keys.
    - social_values: list. List to store social science metadata values.
    - astronomy_keys: list. List to store astronomy metadata keys.
    - astronomy_values: list. List to store astronomy metadata values.
    - biomedical_keys: list. List to store biomedical metadata keys.
    - biomedical_values: list. List to store biomedical metadata values.
    - journal_keys: list. List to store journal metadata keys.
    - journal_values: list. List to store journal metadata values.
    - computationalworkflow_keys: list. List to store computational workflow metadata keys.
    - computationalworkflow_values: list. List to store computational workflow metadata values.
    - LocalContextsCVoc_keys: list. List to store local contexts metadata keys.
    - LocalContextsCVoc_values: list. List to store local contexts metadata values.
    - darwincore_keys: list. List to store darwin core metadata keys.
    - darwincore_values: list. List to store darwin core metadata values.

    Returns:
    - None. Updates the provided lists with extracted metadata.
    """
    from pyDataverse.api import NativeApi
    import os

    # Instantiate API objects for accessing Dataverse
    api = NativeApi(base_url, token)

    try:
        # Retrieve dataset metadata
        dataset = api.get_dataset(doi)

        # Extract citation metadata if available
        if 'citation' in dataset.json()['data']['latestVersion']['metadataBlocks']:
            metadata_citation = dataset.json()['data']['latestVersion']['metadataBlocks']['citation']['fields']
            citation = extract_value(metadata_citation)
            citation_keys.extend(citation[0])
            citation_values.extend(citation[1])
            for item in metadata_citation:
                if isinstance(item['value'], str):
                    index_canvi = citation_keys.index(item['typeName'])
                    citation_values[index_canvi] = item['value']

        # Extract geospatial metadata if available
        if 'geospatial' in dataset.json()['data']['latestVersion']['metadataBlocks']:
            metadata_geospatial = dataset.json()['data']['latestVersion']['metadataBlocks']['geospatial']['fields']
            geospatial = extract_value(metadata_geospatial)
            geo_keys.extend(geospatial[0])
            geo_values.extend(geospatial[1])
            for item in metadata_geospatial:
                if isinstance(item['value'], str):
                    index_canvi = geo_keys.index(item['typeName'])
                    geo_values[index_canvi] = item['value']

        # Extract social science metadata if available
        if 'socialscience' in dataset.json()['data']['latestVersion']['metadataBlocks']:
            metadata_socialscience = dataset.json()['data']['latestVersion']['metadataBlocks']['socialscience']['fields']
            socialscience = extract_value(metadata_socialscience)
            social_keys.extend(socialscience[0])
            social_values.extend(socialscience[1])
            for item in metadata_socialscience:
                if isinstance(item['value'], str):
                    index_canvi = social_keys.index(item['typeName'])
                    social_values[index_canvi] = item['value']

        # Extract astronomy metadata if available
        if 'astrophysics' in dataset.json()['data']['latestVersion']['metadataBlocks']:
            metadata_astronomy = dataset.json()['data']['latestVersion']['metadataBlocks']['astrophysics']['fields']
            astronomy = extract_value(metadata_astronomy)
            astronomy_keys.extend(astronomy[0])
            astronomy_values.extend(astronomy[1])
            for item in metadata_astronomy:
                if isinstance(item['value'], str):
                    index_canvi = astronomy_keys.index(item['typeName'])
                    astronomy_values[index_canvi] = item['value']

        # Extract biomedical metadata if available
        if 'biomedical' in dataset.json()['data']['latestVersion']['metadataBlocks']:
            metadata_biomedical = dataset.json()['data']['latestVersion']['metadataBlocks']['biomedical']['fields']
            biomedical = extract_value(metadata_biomedical)
            biomedical_keys.extend(biomedical[0])
            biomedical_values.extend(biomedical[1])
            for item in metadata_biomedical:
                if isinstance(item['value'], str):
                    index_canvi = biomedical_keys.index(item['typeName'])
                    biomedical_values[index_canvi] = item['value']

        # Extract journal metadata if available
        if 'journal' in dataset.json()['data']['latestVersion']['metadataBlocks']:
            metadata_journal = dataset.json()['data']['latestVersion']['metadataBlocks']['journal']['fields']
            journal = extract_value(metadata_journal)
            journal_keys.extend(journal[0])
            journal_values.extend(journal[1])
            for item in metadata_journal:
                if isinstance(item['value'], str):
                    index_canvi = journal_keys.index(item['typeName'])
                    journal_values[index_canvi] = item['value']

        # Extract computationalworkflow metadata if available
        if 'computationalworkflow' in dataset.json()['data']['latestVersion']['metadataBlocks']:
            metadata_computationalworkflow = dataset.json()['data']['latestVersion']['metadataBlocks']['computationalworkflow']['fields']
            computationalworkflow = extract_value(metadata_computationalworkflow)
            computationalworkflow_keys.extend(computationalworkflow[0])
            computationalworkflow_values.extend(computationalworkflow[1])
            for item in metadata_computationalworkflow:
                if isinstance(item['value'], str):
                    index_canvi = computationalworkflow_keys.index(item['typeName'])
                    computationalworkflow_values[index_canvi] = item['value']

        # Extract LocalContextsCVoc metadata if available
        if 'LocalContextsCVoc' in dataset.json()['data']['latestVersion']['metadataBlocks']:
            metadata_LocalContextsCVoc = dataset.json()['data']['latestVersion']['metadataBlocks']['LocalContextsCVoc']['fields']
            LocalContextsCVoc = extract_value(metadata_LocalContextsCVoc)
            LocalContextsCVoc_keys.extend(LocalContextsCVoc[0])
            LocalContextsCVoc_values.extend(LocalContextsCVoc[1])
            for item in metadata_LocalContextsCVoc:
                if isinstance(item['value'], str):
                    index_canvi = LocalContextsCVoc_keys.index(item['typeName'])
                    LocalContextsCVoc_values[index_canvi] = item['value']

        # Extract darwincore metadata if available
        if 'darwincore' in dataset.json()['data']['latestVersion']['metadataBlocks']:
            metadata_darwincore = dataset.json()['data']['latestVersion']['metadataBlocks']['darwincore']['fields']
            darwincore = extract_value(metadata_darwincore)
            darwincore_keys.extend(darwincore[0])
            darwincore_values.extend(darwincore[1])
            for item in metadata_darwincore:
                if isinstance(item['value'], str):
                    index_canvi = darwincore_keys.index(item['typeName'])
                    darwincore_values[index_canvi] = item['value']


    except (KeyError, InvalidSchema) as e:
        # Catch specific exceptions and print the error message
        print(f"Error occurred: {e}")
        print('There was an error reading metadata for the dataset: ' + doi)

def filemetadata(base_url, token, doi, filemetadata_keys, filemetadata_values):
    """
    Function to extract metadata for files associated with a dataset identified by its DOI.

    Parameters:
    - base_url: str. Base URL of the Dataverse instance.
    - token: str. API token for authentication.
    - doi: str. DOI of the dataset.
    - filemetadata_keys: list. List to store file metadata keys.
    - filemetadata_values: list. List to store file metadata values.

    Returns:
    - None. Updates the provided lists with extracted file metadata.
    """
    from pyDataverse.api import NativeApi

    # Instantiate API objects for accessing Dataverse
    api = NativeApi(base_url, token)

    try:
        # Retrieve dataset metadata
        dataset = api.get_dataset(doi)

        # Iterate through files and extract metadata
        for i in range(len(dataset.json()['data']['latestVersion']['files'])):
            filemetadata_resp = dataset.json()['data']['latestVersion']['files'][i]['dataFile']
            filemetadata_keys_aux = list(filemetadata_resp.keys())
            filemetadata_values_aux = list(filemetadata_resp.values())
            filemetadata_keys.append(filemetadata_keys_aux)
            filemetadata_values.append(filemetadata_values_aux)
    except KeyError:
        print('There was an error reading metadata for the files of the dataset: ' + doi)

def list_duplicates_of(seq, item):
    """
    Function to list indexes of duplicates of an item in a sequence.

    Parameters:
    - seq: list. Sequence to search for duplicates.
    - item: any. Item to search for duplicates.

    Returns:
    - locs: list. List of indexes where the item occurs more than once in the sequence.
    """
    start_at = -1
    locs = []
    while True:
        try:
            loc = seq.index(item, start_at + 1)
        except ValueError:
            break
        else:
            locs.append(loc)
            start_at = loc
    return locs

def find_keys(keys, specified_keys, values):
    """
    Function to find specified keys and their corresponding values in a list of keys and values.

    Parameters:
    - keys: list. List of keys.
    - specified_keys: list. List of specified keys to find.
    - values: list. List of values corresponding to the keys.

    Returns:
    - extracted_values: list. List of dictionaries containing extracted key-value pairs.
    """
    # Dictionary to store extracted values
    extracted_values = []
    # Dictionary to store current entry
    current_entry = {}
    # Set to keep track of found keys
    found_keys = set()
    # Iterate through keys and values
    for key, value in zip(keys, values):
        # Check if current key is in specified keys
        if key in specified_keys:
            current_entry[key] = value
            found_keys.add(key)
            # If all specified keys are found, add entry to extracted_values
            if len(found_keys) == len(specified_keys):
                extracted_values.append(current_entry)
                current_entry = {}  # Reset current entry
                found_keys.clear()  # Clear found keys set for next entry
    return extracted_values

'''
def format_key(key):
    """
    Function to format a key by splitting camel case and capitalizing the first letter of each word.

    Parameters:
    - key: str. Key to be formatted.

    Returns:
    - formatted_key: str. Formatted key.
    """
    words = []
    current_word = ''
    for char in key:
        if char.isupper() and current_word:
            words.append(current_word)
            current_word = char
        else:
            current_word += char
    if current_word:
        words.append(current_word)

    formatted_key = ' '.join(words)
    return formatted_key.capitalize()
'''

def createreadme(base_url, token, doi,
                 citation_keys, citation_values,
                 geo_keys, geo_values,
                 social_keys, social_values,
                 astronomy_keys, astronomy_values,
                 biomedical_keys, biomedical_values,
                 journal_keys, journal_values,
                 computationalworkflow_keys, computationalworkflow_values,
                 LocalContextsCVoc_keys,LocalContextsCVoc_values,
                 darwincore_keys, darwincore_values,
                 filemetadata_keys, filemetadata_values):
    """
    Function to create a readme file for a dataset.

    Parameters:
    - base_url: str. Base URL of the Dataverse instance.
    - token: str. API token for authentication.
    - doi: str. DOI of the dataset.
    - citation_keys: list. List of citation metadata keys.
    - citation_values: list. List of citation metadata values.
    - geo_keys: list. List of geospatial metadata keys.
    - geo_values: list. List of geospatial metadata values.
    - social_keys: list. List of social science metadata keys.
    - social_values: list. List of social science metadata values.
    - astronomy_keys: list. List of astronomy metadata keys.
    - astronomy_values: list. List of astronomy metadata values.
    - biomedical_keys: list. List of biomedical metadata keys.
    - biomedical_values: list. List of biomedical metadata values.
    - journal_keys: list. List of journal metadata keys.
    - journal_values: list. List of journal metadata values.
    - filemetadata_keys: list. List of file metadata keys.
    - filemetadata_values: list. List of file metadata values.
    """

    # Import necessary libraries
    from jproperties import Properties
    from pyDataverse.api import NativeApi
    import os

    # Instantiate API objects for accessing Dataverse
    api = NativeApi(base_url, token)

    # Read the properties files
    bundleProperties = Properties()
    citationProperties = Properties()
    journalProperties = Properties()
    geoProperties = Properties()
    socialProperties = Properties()
    astroProperties = Properties()
    bioProperties = Properties()
    journalProperties = Properties()
    computationalProperties = Properties()
    citationPropertiesBack = Properties()
    bundlePropertiesBack = Properties()
    journalPropertiesBack = Properties()
    geoPropertiesBack = Properties()
    socialPropertiesBack = Properties()
    astroPropertiesBack = Properties()
    bioPropertiesBack = Properties()
    journalPropertiesBack = Properties()
    computationalPropertiesBack = Properties()
    if os.path.exists(properties_path + 'Bundle_' + language + '.properties'):
        with open(properties_path + 'Bundle_' + language + '.properties', 'rb') as read_prop: 
            bundleProperties.load(read_prop,"utf-8")
    else:
        with open(properties_path + 'Bundle.properties', 'rb') as read_prop: 
            bundleProperties.load(read_prop,"utf-8")
    if os.path.exists(properties_path + 'citation_' + language + '.properties'):
        with open(properties_path + 'citation_' + language + '.properties', 'rb') as read_prop: 
            citationProperties.load(read_prop,"utf-8")
    else:
        with open(properties_path + 'citation.properties', 'rb') as read_prop: 
            citationProperties.load(read_prop,"utf-8")
    if os.path.exists(properties_path + 'journal_' + language + '.properties'):
        with open(properties_path + 'journal_' + language + '.properties', 'rb') as read_prop: 
            journalProperties.load(read_prop,"utf-8")
    else:
        with open(properties_path + 'journal.properties', 'rb') as read_prop: 
            journalProperties.load(read_prop,"utf-8")
    if os.path.exists(properties_path + 'geospatial_' + language + '.properties'):
        with open(properties_path + 'geospatial_' + language + '.properties', 'rb') as read_prop: 
            geoProperties.load(read_prop,"utf-8")
    else:
        with open(properties_path + 'geospatial.properties', 'rb') as read_prop: 
            geoProperties.load(read_prop,"utf-8")
    if os.path.exists(properties_path + 'socialscience_' + language + '.properties'):
        with open(properties_path + 'socialscience_' + language + '.properties', 'rb') as read_prop: 
            socialProperties.load(read_prop,"utf-8")
    else:
        with open(properties_path + 'socialscience.properties', 'rb') as read_prop: 
            socialProperties.load(read_prop,"utf-8")
    if os.path.exists(properties_path + 'astrophysics_' + language + '.properties'):
        with open(properties_path + 'astrophysics_' + language + '.properties', 'rb') as read_prop: 
            astroProperties.load(read_prop,"utf-8")
    else:
        with open(properties_path + 'astrophysics.properties', 'rb') as read_prop: 
            astroProperties.load(read_prop,"utf-8")
    if os.path.exists(properties_path + 'biomedical_' + language + '.properties'):
        with open(properties_path + 'biomedical_' + language + '.properties', 'rb') as read_prop: 
            bioProperties.load(read_prop,"utf-8")
    else:
        with open(properties_path + 'biomedical.properties', 'rb') as read_prop: 
            bioProperties.load(read_prop,"utf-8")
    if os.path.exists(properties_path + 'computationalworkflow_' + language + '.properties'):
        with open(properties_path + 'computationalworkflow_' + language + '.properties', 'rb') as read_prop: 
            computationalProperties.load(read_prop,"utf-8")
    else:
        with open(properties_path + 'computationalworkflow.properties', 'rb') as read_prop: 
            computationalProperties.load(read_prop,"utf-8")

    with open(properties_path + 'Bundle.properties', 'rb') as read_prop: 
        bundlePropertiesBack.load(read_prop,"utf-8")
    with open(properties_path + 'citation.properties', 'rb') as read_prop: 
        citationPropertiesBack.load(read_prop,"utf-8")
    with open(properties_path + 'journal.properties', 'rb') as read_prop: 
        journalPropertiesBack.load(read_prop,"utf-8")
    with open(properties_path + 'geospatial.properties', 'rb') as read_prop: 
        geoPropertiesBack.load(read_prop,"utf-8")
    with open(properties_path + 'socialscience.properties', 'rb') as read_prop: 
        socialPropertiesBack.load(read_prop,"utf-8")
    with open(properties_path + 'astrophysics.properties', 'rb') as read_prop: 
        astroPropertiesBack.load(read_prop,"utf-8")
    with open(properties_path + 'biomedical.properties', 'rb') as read_prop: 
        bioPropertiesBack.load(read_prop,"utf-8")
    with open(properties_path + 'computationalworkflow.properties', 'rb') as read_prop: 
        computationalPropertiesBack.load(read_prop,"utf-8")

    # Retrieve dataset metadata
    dataset = api.get_dataset(doi)

    # Extract path from DOI
    #path = dest_dir + doi.replace("doi:10.21950/", "")
    path = dest_dir + identifier;

    try:
        # Create directory if it does not exist
        os.makedirs(path)
    except OSError:
        print("Directory " + path + ' already exists. The Readme will be saved in this directory.')

    with open(path + '/' + 'readme_' + language + '.txt', 'w', encoding='utf-8') as f:
        text = translate (bundleProperties, bundlePropertiesBack, 'dataverse.option.generalInfo', False, True);
        f.write(text + '\n------------------\n')
        cont = 0

        # Write metadata to Readme file
        if 'PreviousDatasetPersistentID' in citation_keys:
            cont += 1
            text = translate (bundleProperties, bundlePropertiesBack, 'dataset.metadata.alternativePersistentId', False, False);
            f.write(str(cont) + '.' +  text +':\n')
            auxiliar = []
            auxiliar.append(list_duplicates_of(citation_keys, 'PreviousDatasetPersistentID'))
            for i in auxiliar[0]:
                f.write('\t')
                f.write(citation_values[i])
                if i != auxiliar[0][-1]:
                    f.write('\n ')
            f.write('\n\n')

        if 'title' in citation_keys:
            cont += 1
            text = translate (bundleProperties, bundlePropertiesBack, 'createReadme.datasetTitle', False, False);
            f.write(str(cont) + '.  ' + text +':\n')
            f.write('\t' + citation_values[citation_keys.index('title')] + '\n\n')  # Write the title

        if 'authorName' in citation_keys or 'authorAffiliation' in citation_keys or 'authorIdentifierScheme' in citation_keys or 'authorIdentifier' in citation_keys:
            cont+=1
            target_keys = ['authorName', 'authorAffiliation', 'authorIdentifierScheme', 'authorIdentifier']
            # 1. Chunk citation_keys and citation_values by author using 'authorName'
            author_chunks = []
            current_keys, current_vals = [], []

            for k, v in zip(citation_keys, citation_values):
                if k == 'authorName' and current_keys:
                    author_chunks.append((current_keys, current_vals))
                    current_keys, current_vals = [], []
                current_keys.append(k)
                current_vals.append(v)

            if current_keys:
                author_chunks.append((current_keys, current_vals))

            # 2. Call find_keys on each author chunk separately
            extracted_values = []
            for chunk_keys, chunk_vals in author_chunks:
                # Only ask find_keys for keys that ACTUALLY exist in this author's chunk
                specified_keys = [k for k in target_keys if k in chunk_keys]

                # find_keys works without issue because length checks match perfectly
                chunk_result = find_keys(chunk_keys, specified_keys, chunk_vals)
                extracted_values.extend(chunk_result)

            # 3. Write output
            for entry in extracted_values:
                for key, value in entry.items():
                    formatted_key = translate(citationProperties, citationPropertiesBack, 'datasetfieldtype.' + key + '.title', False, False)
                    f.write(f'\t{formatted_key}: {value}\n')
                f.write('\n')

        if 'datasetContactName' in citation_keys or 'datasetContactAffiliation' in citation_keys or 'datasetContactEmail' in citation_keys:
            cont+=1
            text = translate (bundleProperties, bundlePropertiesBack, 'createReadme.datasetContact', False, False);
            f.write(str(cont)+'.  ' + text + ':\n')
            keys=['datasetContactName','datasetContactAffiliation','datasetContactEmail' ]
            specified_keys = [element for element in keys if element in citation_keys]
            extracted_values = find_keys(citation_keys, specified_keys, citation_values)
            for entry in extracted_values:
                for key, value in entry.items():
                    formatted_key = translate (citationProperties, citationPropertiesBack, 'datasetfieldtype.' + key + '.title', False, False);
                    f.write('\t'+f'{formatted_key}: {value}\n')
                f.write('\n')
        text = translate (bundleProperties, bundlePropertiesBack, 'description', False, True);
        f.write(text + '\n----------\n')
        cont=0
        if 'language' in citation_keys:
            cont+=1
            text = translate (bundleProperties, bundlePropertiesBack, 'createReadme.datasetLanguage', False, False);
            f.write(str(cont)+'.  ' + text + ':\n')
            auxiliar=[]
            auxiliar.append(list_duplicates_of(citation_keys, 'language'))
            for i in auxiliar[0]:
                f.write('\t')
                text = translate (citationProperties, citationPropertiesBack, 'controlledvocabulary.language.' + citation_values[i].lower().replace(' ', '_'), False, False);
                f.write(text)
                if i != auxiliar[0][-1]:
                    f.write('\n ')
            f.write('\n\n')
        if 'dsDescriptionValue' in citation_keys:
            cont+=1
            text = translate (journalProperties, journalPropertiesBack, 'controlledvocabulary.journalArticleType.abstract', False, False);
            f.write(str(cont)+'.  ' + text + ':\n')
            auxiliar=[]
            auxiliar.append(list_duplicates_of(citation_keys, 'dsDescriptionValue'))
            for i in auxiliar[0]:
                f.write('\t')
                f.write(fromstring(citation_values[i]).text_content()) # MADROÑO. Remove html tags from description
                if i != auxiliar[0][-1]:
                    f.write('\n ')
            f.write('\n\n')
        if 'subject' in citation_keys:
            cont+=1
            text = translate (bundleProperties, bundlePropertiesBack, 'dataset.subjectDisplay.title', False, False);
            f.write(str(cont)+'.  ' + text + ':\n')
            auxiliar=[]
            auxiliar.append(list_duplicates_of(citation_keys, 'subject'))
            for i in auxiliar[0]:
                f.write('\t')
                tr_subject= translate (citationProperties, citationPropertiesBack, 'controlledvocabulary.subject.' + citation_values[i].lower().replace(' ', '_'), False, False)
                f.write(tr_subject)
                #f.write(citation_values[i])
                if i != auxiliar[0][-1]:
                    f.write('\n ')
            f.write('\n\n')
        if 'keywordValue' in citation_keys or 'keywordVocabulary' in citation_keys or 'keywordVocabularyURI' in citation_keys:
            cont+=1
            text = translate (bundleProperties, bundlePropertiesBack, 'dataset.keywordDisplay.title', False, False);
            f.write(str(cont)+'.  ' + text + ':\n')
            keys=['keywordValue','keywordVocabulary','keywordVocabularyURI' ]
            specified_keys = [element for element in keys if element in citation_keys]
            extracted_values = find_keys(citation_keys, specified_keys, citation_values)
            for entry in extracted_values:
                for key, value in entry.items():
                    formatted_key = translate (citationProperties, citationPropertiesBack, 'datasetfieldtype.' + key + '.title', False, False);
                    f.write('\t'+f'{formatted_key}: {value}\n')
                f.write('\n')
        if 'topicClassValue' in citation_keys or 'topicClassVocab' in citation_keys or 'topicClassVocabURI' in citation_keys:
            cont+=1
            text = translate (bundleProperties, bundlePropertiesBack, 'dataset.topicClassification.title', False, False);
            f.write(str(cont)+'.  ' + text + ':\n')
            keys=['topicClassValue','topicClassVocab','topicClassVocabURI' ]
            specified_keys = [element for element in keys if element in citation_keys]
            extracted_values = find_keys(citation_keys, specified_keys, citation_values)
            for entry in extracted_values:
                for key, value in entry.items():
                    formatted_key = translate (citationProperties, citationPropertiesBack, 'datasetfieldtype.' + key + '.title', False, False);
                    f.write('\t'+f'{formatted_key}: {value}\n')
                f.write('\n')
        if 'notesText' in citation_keys:
            cont+=1
            text = translate (citationProperties, citationPropertiesBack, 'datasetfieldtype.notesText.title', False, False);
            f.write(str(cont)+'.  ' + text + ':\n')
            f.write('\t'+citation_values[citation_keys.index('notesText')]+'\n\n')
        if 'producerName' in citation_keys or 'producerAffiliation' in citation_keys or 'producerAbbreviation' in citation_keys or 'producerURL' in citation_keys:
            cont+=1
            text = translate (citationProperties, citationPropertiesBack, 'datasetfieldtype.producer.title', False, False);
            f.write(str(cont)+'.  ' + text + ':\n')
            keys=['producerName','producerAffiliation','producerAbbreviation','producerURL' ]
            specified_keys = [element for element in keys if element in citation_keys]
            extracted_values = find_keys(citation_keys, specified_keys, citation_values)
            for entry in extracted_values:
                for key, value in entry.items():
                    formatted_key = translate (citationProperties, citationPropertiesBack, 'datasetfieldtype.' + key + '.title', False, False);
                    f.write('\t'+f'{formatted_key}: {value}\n')
                f.write('\n')
        if 'productionDate' in citation_keys:
            cont+=1
            text = translate (citationProperties, citationPropertiesBack, 'datasetfieldtype.productionDate.title', False, False);
            f.write(str(cont)+'.  ' + text + ':\n')
            f.write('\t'+citation_values[citation_keys.index('productionDate')]+'\n\n')
        if 'Production place' in citation_keys:
            cont+=1
            text = translate (citationProperties, citationPropertiesBack, 'datasetfieldtype.productionPlace.title', False, False);
            f.write(str(cont)+'.  ' + text + ':\n')
            f.write('\t'+citation_values[citation_keys.index('productionPlace')]+'\n\n')
        if 'contributorType' in citation_keys or 'contributorName' in citation_keys:
            cont+=1
            text = translate (citationProperties, citationPropertiesBack, 'datasetfieldtype.contributor.title', False, False);
            f.write(str(cont)+'.  ' + text + ':\n')
            keys=['contributorType','contributorName']
            specified_keys = [element for element in keys if element in citation_keys]
            extracted_values = find_keys(citation_keys, specified_keys, citation_values)
            for entry in extracted_values:
                for key, value in entry.items():
                    formatted_key = translate (citationProperties, citationPropertiesBack, 'datasetfieldtype.' + key + '.title', False, False);
                    f.write('\t'+f'{formatted_key}: {value}\n')
                f.write('\n')
        if 'grantNumberAgency' in citation_keys or 'grantNumberValue' in citation_keys:
            cont+=1
            text = translate (bundleProperties, bundlePropertiesBack, 'createReadme.grantInformation', False, False);
            f.write(str(cont)+'.  ' + text + ':\n')
            keys=['grantNumberAgency','grantNumberValue']
            specified_keys = [element for element in keys if element in citation_keys]
            extracted_values = find_keys(citation_keys, specified_keys, citation_values)
            for entry in extracted_values:
                for key, value in entry.items():
                    formatted_key = translate (citationProperties, citationPropertiesBack, 'datasetfieldtype.' + key + '.title', False, False);
                    f.write('\t'+f'{formatted_key}: {value}\n')
                f.write('\n')
        if 'distributorName' in citation_keys or 'distributorAffiliation' in citation_keys or 'distributorAbbreviation' in citation_keys or 'distributorURL' in citation_keys:
            cont+=1
            text = translate (citationProperties, citationPropertiesBack, 'datasetfieldtype.distributor.title', False, False);
            f.write(str(cont)+'.  ' + text + ':\n')
            keys=['distributorName','distributorAffiliation','distributorAbbreviation','distributorURL' ]
            specified_keys = [element for element in keys if element in citation_keys]
            extracted_values = find_keys(citation_keys, specified_keys, citation_values)
            for entry in extracted_values:
                for key, value in entry.items():
                    formatted_key = translate (citationProperties, citationPropertiesBack, 'datasetfieldtype.' + key + '.title', False, False);
                f.write('\n')
        if 'distributionDate' in citation_keys:
            cont+=1
            text = translate (citationProperties, citationPropertiesBack, 'datasetfieldtype.distributionDate.title', False, False);
            f.write(str(cont)+'.  ' + text + ':\n')
            f.write('\t'+citation_values[citation_keys.index('distributionDate')]+'\n\n')
        if 'depositor' in citation_keys:
            cont+=1
            text = translate (citationProperties, citationPropertiesBack, 'datasetfieldtype.depositor.title', False, False);
            f.write(str(cont)+'.  ' + text + ':\n')
            f.write('\t'+citation_values[citation_keys.index('depositor')]+'\n\n')
        if 'dateOfDeposit' in citation_keys:
            cont+=1
            text = translate (bundleProperties, bundlePropertiesBack, 'file.metadataTab.fileMetadata.depositDate.label', False, False);
            f.write(str(cont)+'.  ' + text + ':\n')
            f.write('\t'+citation_values[citation_keys.index('dateOfDeposit')]+'\n\n')
        if 'timePeriodCoveredStart' in citation_keys or 'timePeriodCoveredEnd' in citation_keys:
            cont+=1
            text = translate (bundleProperties, bundlePropertiesBack, 'createReadme.timePeriodCovered', False, False);
            f.write(str(cont)+'.  ' + text + ':\n')
            keys=['timePeriodCoveredStart','timePeriodCoveredEnd']
            specified_keys = [element for element in keys if element in citation_keys]
            extracted_values = find_keys(citation_keys, specified_keys, citation_values)
            for entry in extracted_values:
                for key, value in entry.items():
                    formatted_key = translate (citationProperties, citationPropertiesBack, 'datasetfieldtype.' + key + '.title', False, False);
                    f.write('\t'+f'{formatted_key}: {value}\n')
                f.write('\n')
        if 'dateOfCollectionStart' in citation_keys or 'dateOfCollectionEnd' in citation_keys:
            cont+=1
            text = translate (bundleProperties, bundlePropertiesBack, 'createReadme.dateOfCollection', False, False);
            f.write(str(cont)+'.  ' + text + ':\n')
            keys=['dateOfCollectionStart','dateOfCollectionEnd']
            specified_keys = [element for element in keys if element in citation_keys]
            extracted_values = find_keys(citation_keys, specified_keys, citation_values)
            for entry in extracted_values:
                for key, value in entry.items():
                    formatted_key = translate (citationProperties, citationPropertiesBack, 'datasetfieldtype.' + key + '.title', False, False);
                    f.write('\t'+f'{formatted_key}: {value}\n')
                f.write('\n')
        if 'publicationDate' in dataset.json()['data']:
            cont+=1
            text = translate (journalProperties, journalPropertiesBack, 'datasetfieldtype.journalPubDate.title', False, False);
            f.write(str(cont)+'.  ' + text + ':\n')
            f.write('\t'+dataset.json()['data']['publicationDate']+'\n\n')
        if 'dateOfCollectionStart' not in citation_keys and 'publicationDate' not in dataset.json()['data']:
            f.write('\n')
        if 'kindOfData' in citation_keys:
            cont+=1
            text = translate (citationProperties, citationPropertiesBack, 'datasetfieldtype.kindOfData.title', False, False);
            f.write(str(cont)+'.  ' + text + ':\n')
            auxiliar=[]
            auxiliar.append(list_duplicates_of(citation_keys, 'kindOfData'))
            for i in auxiliar[0]:
                f.write('\t')
                f.write(citation_values[i])
                if i != auxiliar[0][-1]:
                    f.write('\n ')
            f.write('\n\n')
        if 'seriesName' in citation_keys or 'seriesInformation' in citation_keys:
            cont+=1
            text = translate (citationProperties, citationPropertiesBack, 'datasetfieldtype.series.title', False, False);
            f.write(str(cont)+'.  ' + text + ':\n')
            index=[]
            keys=['seriesName','seriesInformation']
            specified_keys=[]
            specified_keys = [element for element in keys if element in citation_keys]
            extracted_values = find_keys(citation_keys, specified_keys, citation_values)
            for entry in extracted_values:
                for key, value in entry.items():
                    formatted_key = translate (citationProperties, citationPropertiesBack, 'datasetfieldtype.' + key + '.title', False, False);
                    f.write('\t'+f'{formatted_key}: {value}\n')
                f.write('\n')
        if 'softwareName' in citation_keys or 'softwareVersion' in citation_keys:
            cont+=1
            text = translate (citationProperties, citationPropertiesBack, 'datasetfieldtype.software.title', False, False);
            f.write(str(cont)+'.  ' + text + ':\n')
            keys=['softwareName','softwareVersion']
            specified_keys = [element for element in keys if element in citation_keys]
            extracted_values = find_keys(citation_keys, specified_keys, citation_values)
            for entry in extracted_values:
                for key, value in entry.items():
                    formatted_key = translate (citationProperties, citationPropertiesBack, 'datasetfieldtype.' + key + '.title', False, False);
                    f.write('\t'+f'{formatted_key}: {value}\n')
                f.write('\n')
        if 'relatedMaterial' in citation_keys:
            cont+=1
            text = translate (citationProperties, citationPropertiesBack, 'datasetfieldtype.relatedMaterial.title', False, False);
            f.write(str(cont)+'.  ' + text + ':\n')
            auxiliar=[]
            auxiliar.append(list_duplicates_of(citation_keys, 'relatedMaterial'))
            for i in auxiliar[0]:
                f.write('\t')
                f.write(citation_values[i])
                if i != auxiliar[0][-1]:
                    f.write('\n ')
            f.write('\n\n')
        if 'relatedDatasets' in citation_keys:
            cont+=1
            text = translate (citationProperties, citationPropertiesBack, 'datasetfieldtype.relatedDatasets.title', False, False);
            f.write(str(cont)+'.  ' + text + ':\n')
            auxiliar=[]
            auxiliar.append(list_duplicates_of(citation_keys, 'relatedDatasets'))
            for i in auxiliar[0]:
                f.write('\t')
                f.write(citation_values[i])
                if i != auxiliar[0][-1]:
                    f.write('\n ')
            f.write('\n\n')
        if 'otherReferences' in citation_keys:
            cont+=1
            text = translate (bundleProperties, bundlePropertiesBack, 'createReadme.otherReferences', False, False);
            f.write(str(cont)+'.  ' + text + ':\n')
            auxiliar=[]
            auxiliar.append(list_duplicates_of(citation_keys, 'otherReferences'))
            for i in auxiliar[0]:
                f.write('\t')
                f.write(citation_values[i])
                if i != auxiliar[0][-1]:
                    f.write('\n ')
            f.write('\n\n')
        if 'dataSources' in citation_keys:
            cont+=1
            text = translate (bundleProperties, bundlePropertiesBack, 'createReadme.dataSources', False, False);
            f.write(str(cont)+'.  ' + text + ':\n')
            auxiliar=[]
            auxiliar.append(list_duplicates_of(citation_keys, 'dataSources'))
            for i in auxiliar[0]:
                f.write('\t')
                f.write(citation_values[i])
                if i != auxiliar[0][-1]:
                    f.write('\n ')
            f.write('\n\n')
        if 'originOfSources' in citation_keys:
            cont+=1
            text = translate (citationProperties, citationPropertiesBack, 'datasetfieldtype.originOfSources.title', False, False);
            f.write(str(cont)+'.  ' + text + ':\n')
            auxiliar=[]
            auxiliar.append(list_duplicates_of(citation_keys, 'originOfSources'))
            for i in auxiliar[0]:
                f.write('\t')
                f.write(citation_values[i])
                if i != auxiliar[0][-1]:
                    f.write('\n ')
            f.write('\n\n')
        if 'characteristicOfSources' in citation_keys:
            cont+=1
            text = translate (citationProperties, citationPropertiesBack, 'datasetfieldtype.characteristicOfSources.title', False, False);
            f.write(str(cont)+'.  ' + text + ':\n')
            auxiliar=[]
            auxiliar.append(list_duplicates_of(citation_keys, 'characteristicOfSources'))
            for i in auxiliar[0]:
                f.write('\t')
                f.write(citation_values[i])
                if i != auxiliar[0][-1]:
                    f.write('\n ')
            f.write('\n\n')
        if 'accessToSources' in citation_keys:
            cont+=1
            text = translate (citationProperties, citationPropertiesBack, 'datasetfieldtype.accessToSources.title', False, False);
            f.write(str(cont)+'.  ' + text + ':\n')
            auxiliar=[]
            auxiliar.append(list_duplicates_of(citation_keys, 'accessToSources'))
            for i in auxiliar[0]:
                f.write('\t')
                f.write(citation_values[i])
                if i != auxiliar[0][-1]:
                    f.write('\n ')
            f.write('\n\n')
        text = translate (bundleProperties, bundlePropertiesBack, 'createReadme.accessInformation', False, False);
        f.write(text + '\n------------------------\n')
        cont=0
        if 'license' in dataset.json()['data']['latestVersion']:
            cont+=1
            text = translate (bundleProperties, bundlePropertiesBack, 'createReadme.datasetLicense', False, False);
            f.write(str(cont)+'.  ' + text + ':\n')
            f.write('\t'+dataset.json()['data']['latestVersion']['license']['name']+'\n\n')
        if 'termsOfUse' in dataset.json()['data']['latestVersion']:
            cont+=1
            text = translate (bundleProperties, bundlePropertiesBack, 'file.dataFilesTab.terms.list.termsOfUse.termsOfUse', False, False);
            f.write(str(cont)+'.  ' + text + ':\n')
            f.write('\t'+dataset.json()['data']['latestVersion']['termsOfUse']+'\n\n')
        if 'persistentUrl' in dataset.json()['data']:
            cont+=1
            text = translate (bundleProperties, bundlePropertiesBack, 'createReadme.datasetDoi', False, False);
            f.write(str(cont)+'.  ' + text + ':\n')
            f.write('\t'+dataset.json()['data']['persistentUrl']+'\n\n')
        if 'publicationCitation' in citation_keys or 'publicationIDType'in citation_keys or 'publicationIDNumber' in citation_keys or 'publicationURL' in citation_keys:
            cont+=1
            text = translate (citationProperties, citationPropertiesBack, 'datasetfieldtype.publication.title', False, False);
            f.write(str(cont)+'.  ' + text + ':\n')
            index=[]
            keys=["publicationRelationType",'publicationCitation','publicationIDType','publicationIDNumber','publicationURL']
            specified_keys = [element for element in keys if element in citation_keys]
            extracted_values = find_keys(citation_keys, specified_keys, citation_values)
            for entry in extracted_values:
                for key, value in entry.items():
                    formatted_key = translate (citationProperties, citationPropertiesBack, 'datasetfieldtype.' + key + '.title', False, False);
                    f.write('\t'+f'{formatted_key}: {value}\n')
                f.write('\n')
        if len(geo_keys) != 0:
            text = translate (geoProperties, geoPropertiesBack, 'metadatablock.displayName', False, False);
            f.write(text + '\n--------------------------------------\n')
            cont=0
            if 'country' in geo_keys or 'state' in geo_keys or 'city' in geo_keys or 'otherGeographicCoverage' in geo_keys:
                cont+=1
                text = translate (bundleProperties, bundlePropertiesBack, 'createReadme.geoLocation', False, False);
                f.write(str(cont)+'.  ' + text + ':\n')
                keys=['country','state','city','otherGeographicCoverage']
                specified_keys=[]
                specified_keys = [element for element in keys if element in geo_keys]
                extracted_values = find_keys(geo_keys, specified_keys, geo_values)
                for entry in extracted_values:
                    for key, value in entry.items():
                        formatted_key = translate (geoProperties, geoPropertiesBack, 'datasetfieldtype.' + key + '.title', False, False);
                        if key == 'country':
                            value= translate (geoProperties, geoPropertiesBack, 'controlledvocabulary.country.' + value.lower().replace (' ', '_'), False, False);
                        f.write('\t'+f'{formatted_key}: {value}\n')
                    f.write('\n')
            if 'geographicUnit' in geo_keys:
                cont+=1
                text = translate (geoProperties, geoPropertiesBack, 'datasetfieldtype.geographicUnit.title', False, False);
                f.write(str(cont)+'.  ' + text + ':\n')
                auxiliar=[]
                auxiliar.append(list_duplicates_of(geo_keys, 'geographicUnit'))
                for i in auxiliar[0]:
                    f.write('\t')
                    f.write(geo_values[i])
                    if i != auxiliar[0][-1]:
                        f.write('\n ')
                f.write('\n\n')
            if 'westLongitude' in geo_keys or 'eastLongitude' in geo_keys or 'northLongitude' in geo_keys or 'southLongitude' in geo_keys:
                keys=['westLongitude','eastLongitude','northLongitude','southLongitude']
                specified_keys=[]
                specified_keys = [element for element in keys if element in geo_keys]
                extracted_values = find_keys(geo_keys, specified_keys, geo_values)
                for entry in extracted_values:
                    for key, value in entry.items():
                        formatted_key = translate (geoProperties, geoPropertiesBack, 'datasetfieldtype.' + key + '.title', False, False);
                        f.write('\t'+f'{formatted_key}: {value}\n')
                    f.write('\n')
        if len(social_keys) != 0:
            text = translate (socialProperties, socialPropertiesBack, 'metadatablock.displayName', False, False);
            f.write(text + '\n--------------------------------------\n')
            cont=0
            if 'unitOfAnalysis' in social_keys:
                cont+=1
                text = translate (socialProperties, socialPropertiesBack, 'datasetfieldtype.unitOfAnalysis.title', False, False);
                f.write(str(cont)+'.  ' + text + ':\n')
                auxiliar=[]
                auxiliar.append(list_duplicates_of(social_keys, 'unitOfAnalysis'))
                for i in auxiliar[0]:
                    f.write('\t')
                    f.write(social_values[i])
                    if i != auxiliar[0][-1]:
                        f.write('\n ')
                f.write('\n\n')
            if 'universe' in social_keys:
                cont+=1
                text = translate (socialProperties, socialPropertiesBack, 'datasetfieldtype.universe.title', False, False);
                f.write(str(cont)+'.  ' + text + ':\n')
                auxiliar=[]
                auxiliar.append(list_duplicates_of(social_keys, 'universe'))
                for i in auxiliar[0]:
                    f.write('\t')
                    f.write(social_values[i])
                    if i != auxiliar[0][-1]:
                        f.write('\n ')
                f.write('\n\n')
            if 'timeMethod' in social_keys:
                cont+=1
                text = translate (socialProperties, socialPropertiesBack, 'datasetfieldtype.timeMethod.title', False, False);
                f.write(str(cont)+'.  ' + text + ':\n')
                f.write('\t'+social_values[social_keys.index('timeMethod')]+'\n\n')
            if 'dataCollector' in social_keys:
                cont+=1
                text = translate (socialProperties, socialPropertiesBack, 'datasetfieldtype.dataCollector.title', False, False);
                f.write(str(cont)+'.  ' + text + ':\n')
                f.write('\t'+social_values[social_keys.index('dataCollector')]+'\n\n')
            if 'collectorTraining' in social_keys:
                cont+=1
                text = translate (socialProperties, socialPropertiesBack, 'datasetfieldtype.collectorTraining.title', False, False);
                f.write(str(cont)+'.  ' + text + ':\n')
                f.write('\t'+social_values[social_keys.index('collectorTraining')]+'\n\n')
            if 'frequencyOfDataCollection' in social_keys:
                cont+=1
                text = translate (socialProperties, socialPropertiesBack, 'datasetfieldtype.frequencyOfDataCollection.title', False, False);
                f.write(str(cont)+'.  ' + text + ':\n')
                f.write('\t'+social_values[social_keys.index('frequencyOfDataCollection')]+'\n\n')
            if 'samplingProcedure' in social_keys:
                cont+=1
                text = translate (socialProperties, socialPropertiesBack, 'datasetfieldtype.samplingProcedure.title', False, False);
                f.write(str(cont)+'.  ' + text + ':\n')
                f.write('\t'+social_values[social_keys.index('samplingProcedure')]+'\n\n')
            if 'targetSampleActualSize' in social_keys or 'targetSampleSizeFormula' in social_keys:
                cont+=1
                text = translate (socialProperties, socialPropertiesBack, 'datasetfieldtype.targetSampleSize.title', False, False);
                f.write(str(cont)+'.  ' + text + ':\n')
                index=[]
                keys=['targetSampleActualSize','targetSampleSizeFormula']
                specified_keys=[]
                specified_keys = [element for element in keys if element in social_keys]
                extracted_values = find_keys(social_keys, specified_keys, social_values)
                for entry in extracted_values:
                    for key, value in entry.items():
                        formatted_key = translate (socialProperties, socialPropertiesBack, 'datasetfieldtype.' + key + '.title', False, False);
                        f.write('\t'+f'{formatted_key}: {value}\n')
                    f.write('\n')
            if 'deviationsFromSampleDesign' in social_keys:
                cont+=1
                text = translate (socialProperties, socialPropertiesBack, 'datasetfieldtype.deviationsFromSampleDesign.title', False, False);
                f.write(str(cont)+'.  ' + text + ':\n')
                f.write('\t'+social_values[social_keys.index('deviationsFromSampleDesign')]+'\n\n')
            if 'collectionMode' in social_keys:
                cont+=1
                text = translate (socialProperties, socialPropertiesBack, 'datasetfieldtype.collectionMode.title', False, False);
                f.write(str(cont)+'.  ' + text + ':\n')
                auxiliar=[]
                auxiliar.append(list_duplicates_of(social_keys, 'collectionMode'))
                for i in auxiliar[0]:
                    f.write('\t')
                    f.write(social_values[i])
                    if i != auxiliar[0][-1]:
                        f.write('\n ')
                f.write('\n\n')
            if 'researchInstrument' in social_keys:
                cont+=1
                text = translate (socialProperties, socialPropertiesBack, 'datasetfieldtype.researchInstrument.title', False, False);
                f.write(str(cont)+'.  ' + text + ':\n')
                f.write('\t'+social_values[social_keys.index('researchInstrument')]+'\n\n')
            if 'dataCollectionSituation' in social_keys:
                cont+=1
                text = translate (socialProperties, socialPropertiesBack, 'datasetfieldtype.dataCollectionSituation.title', False, False);
                f.write(str(cont)+'.  ' + text + ':\n')
                f.write('\t'+social_values[social_keys.index('dataCollectionSituation')]+'\n\n')
            if 'actionsToMinimizeLoss' in social_keys:
                cont+=1
                text = translate (socialProperties, socialPropertiesBack, 'datasetfieldtype.actionsToMinimizeLoss.title', False, False);
                f.write(str(cont)+'.  ' + text + ':\n')
                f.write('\t'+social_values[social_keys.index('actionsToMinimizeLoss')]+'\n\n')
            if 'controlOperations' in social_keys:
                cont+=1
                text = translate (socialProperties, socialPropertiesBack, 'datasetfieldtype.controlOperations.title', False, False);
                f.write(str(cont)+'.  ' + text + ':\n')
                f.write('\t'+social_values[social_keys.index('controlOperations')]+'\n\n')
            if 'weighting' in social_keys:
                cont+=1
                text = translate (socialProperties, socialPropertiesBack, 'datasetfieldtype.weighting.title', False, False);
                f.write(str(cont)+'.  ' + text + ':\n')
                f.write('\t'+social_values[social_keys.index('weighting')]+'\n\n')
            if 'cleaningOperations' in social_keys:
                cont+=1
                text = translate (socialProperties, socialPropertiesBack, 'datasetfieldtype.cleaningOperations.title', False, False);
                f.write(str(cont)+'.  ' + text + ':\n')
                f.write('\t'+social_values[social_keys.index('cleaningOperations')]+'\n\n')
            if 'datasetLevelErrorNotes' in social_keys:
                cont+=1
                text = translate (socialProperties, socialPropertiesBack, 'datasetfieldtype.datasetLevelErrorNotes.title', False, False);
                f.write(str(cont)+'.  ' + text + ':\n')
                f.write('\t'+social_values[social_keys.index('datasetLevelErrorNotes')]+'\n\n')
            if 'responseRate' in social_keys:
                cont+=1
                text = translate (socialProperties, socialPropertiesBack, 'datasetfieldtype.responseRate.title', False, False);
                f.write(str(cont)+'.  ' + text + ':\n')
                f.write('\t'+social_values[social_keys.index('responseRate')]+'\n\n')
            if 'samplingErrorEstimates' in social_keys:
                cont+=1
                text = translate (socialProperties, socialPropertiesBack, 'datasetfieldtype.samplingErrorEstimates.title', False, False);
                f.write(str(cont)+'.  ' + text + ':\n')
                f.write('\t'+social_values[social_keys.index('samplingErrorEstimates')]+'\n\n')
            if 'otherDataAppraisal' in social_keys:
                cont+=1
                text = translate (socialProperties, socialPropertiesBack, 'datasetfieldtype.otherDataAppraisal.title', False, False);
                f.write(str(cont)+'.  ' + text + ':\n')
                f.write('\t'+social_values[social_keys.index('otherDataAppraisal')]+'\n\n')
            if 'socialScienceNotesType' in social_keys or 'socialScienceNotesSubject' in social_keys or 'socialScienceNotesText' in social_keys :
                cont+=1
                text = translate (socialProperties, socialPropertiesBack, 'datasetfieldtype.socialScienceNotes.title', False, False);
                f.write(str(cont)+'.  ' + text + ':\n')
                index=[]
                keys=['socialScienceNotesType','socialScienceNotesSubject','socialScienceNotesText']
                specified_keys=[]
                specified_keys = [element for element in keys if element in social_keys]
                extracted_values = find_keys(social_keys, specified_keys, social_values)
                for entry in extracted_values:
                    for key, value in entry.items():
                        formatted_key = translate (socialProperties, socialPropertiesBack, 'datasetfieldtype.' + key + '.title', False, False);
                        f.write('\t'+f'{formatted_key}: {value}\n')
                    f.write('\n')
        if len(astronomy_keys) != 0:
            text = translate (astroProperties, astroPropertiesBack, 'metadatablock.displayName', False, False);
            f.write(text + '\n--------------------------------------\n')
            cont=0
            if 'astroType' in astronomy_keys:
                cont+=1
                text = translate (astroProperties, astroPropertiesBack, 'datasetfieldtype.astroType.title', False, False);
                f.write(str(cont)+'.  ' + text + ':\n')
                auxiliar=[]
                auxiliar.append(list_duplicates_of(astronomy_keys, 'astroType'))
                for i in auxiliar[0]:
                    f.write('\t')
                    f.write(astronomy_values[i])
                    if i != auxiliar[0][-1]:
                        f.write('\n ')
                f.write('\n\n')
            if 'astroFacility' in astronomy_keys:
                cont+=1
                text = translate (astroProperties, astroPropertiesBack, 'datasetfieldtype.astroFacility.title', False, False);
                f.write(str(cont)+'.  ' + text + ':\n')
                auxiliar=[]
                auxiliar.append(list_duplicates_of(astronomy_keys, 'astroFacility'))
                for i in auxiliar[0]:
                    f.write('\t')
                    f.write(astronomy_values[i])
                    if i != auxiliar[0][-1]:
                        f.write('\n ')
                f.write('\n\n')
            if 'astroInstrument' in astronomy_keys:
                cont+=1
                text = translate (astroProperties, astroPropertiesBack, 'datasetfieldtype.astroInstrument.title', False, False);
                f.write(str(cont)+'.  ' + text + ':\n')
                auxiliar=[]
                auxiliar.append(list_duplicates_of(astronomy_keys, 'astroInstrument'))
                for i in auxiliar[0]:
                    f.write('\t')
                    f.write(astronomy_values[i])
                    if i != auxiliar[0][-1]:
                        f.write('\n ')
                f.write('\n\n')
            if 'astroObject' in astronomy_keys:
                cont+=1
                text = translate (astroProperties, astroPropertiesBack, 'datasetfieldtype.astroObject.title', False, False);
                f.write(str(cont)+'.  ' + text + ':\n')
                auxiliar=[]
                auxiliar.append(list_duplicates_of(astronomy_keys, 'astroObject'))
                for i in auxiliar[0]:
                    f.write('\t')
                    f.write(astronomy_values[i])
                    if i != auxiliar[0][-1]:
                        f.write('\n ')
                f.write('\n\n')
            if 'resolution.Spatial' in astronomy_keys:
                cont+=1
                text = translate (astroProperties, astroPropertiesBack, 'datasetfieldtype.resolution.Spatial.title', False, False);
                f.write(str(cont)+'.  ' + text + ':\n')
                f.write('\t'+astronomy_values[astronomy_keys.index('resolution.Spatial')]+'\n\n')
            if 'resolution.Spectral' in astronomy_keys:
                cont+=1
                text = translate (astroProperties, astroPropertiesBack, 'datasetfieldtype.esolution.Spectral.title', False, False);
                f.write(str(cont)+'.  ' + text + ':\n')
                f.write('\t'+astronomy_values[astronomy_keys.index('resolution.Spectral')]+'\n\n')
            if 'resolution.Temporal' in astronomy_keys:
                cont+=1
                text = translate (astroProperties, astroPropertiesBack, 'datasetfieldtype.resolution.Temporal.title', False, False);
                f.write(str(cont)+'.  ' + text + ':\n')
                f.write('\t'+astronomy_values[astronomy_keys.index('resolution.Temporal')]+'\n\n')
            if 'coverage.Spectral.Bandpass' in astronomy_keys:
                cont+=1
                text = translate (astroProperties, astroPropertiesBack, 'datasetfieldtype.Spectral.Bandpass.title', False, False);
                f.write(str(cont)+'.  ' + text + ':\n')
                auxiliar=[]
                auxiliar.append(list_duplicates_of(astronomy_keys, 'coverage.Spectral.Bandpass'))
                for i in auxiliar[0]:
                    f.write('\t')
                    f.write(astronomy_values[i])
                    if i != auxiliar[0][-1]:
                        f.write('\n ')
                f.write('\n\n')
            if 'coverage.Spectral.CentralWavelength' in astronomy_keys:
                cont+=1
                text = translate (astroProperties, astroPropertiesBack, 'datasetfieldtype.CentralWavelength.title', False, False);
                f.write(str(cont)+'.  ' + text + ':\n')
                auxiliar=[]
                auxiliar.append(list_duplicates_of(astronomy_keys, 'coverage.Spectral.CentralWavelength'))
                for i in auxiliar[0]:
                    f.write('\t')
                    f.write(astronomy_values[i])
                    if i != auxiliar[0][-1]:
                        f.write('\n ')
                f.write('\n\n')
            if 'coverage.Spectral.MinimumWavelength' in astronomy_keys or 'coverage.Spectral.MaximumWavelength' in astronomy_keys:
                cont+=1
                text = translate (astroProperties, astroPropertiesBack, 'datasetfieldtype.Spectral.Wavelength.title', False, False);
                f.write(str(cont)+'.  ' + text + ':\n')
                index=[]
                keys=['coverage.Spectral.MinimumWavelength','coverage.Spectral.MaximumWavelength']
                specified_keys=[]
                specified_keys = [element for element in keys if element in astronomy_keys]
                extracted_values = find_keys(astronomy_keys, specified_keys, astronomy_values)
                for entry in extracted_values:
                    for key, value in entry.items():
                        formatted_key = translate (astroProperties, astroPropertiesBack, 'datasetfieldtype.' + key + '.title', False, False);
                        f.write('\t'+f'{formatted_key}: {value}\n')
                    f.write('\n')
            if  'coverage.Temporal.StartTime' in astronomy_keys or  'coverage.Temporal.StopTime' in astronomy_keys:
                cont+=1
                text = translate (astroProperties, astroPropertiesBack, 'datasetfieldtype.coverage.Temporal.title', False, False);
                f.write(str(cont)+'.  ' + text + ':\n')
                index=[]
                keys=['coverage.Temporal.StartTime', 'coverage.Temporal.StartTime']
                specified_keys=[]
                specified_keys = [element for element in keys if element in astronomy_keys]
                extracted_values = find_keys(astronomy_keys, specified_keys, astronomy_values)
                for entry in extracted_values:
                    for key, value in entry.items():
                        formatted_key = translate (astroProperties, astroPropertiesBack, 'datasetfieldtype.' + key + '.title', False, False);
                        f.write('\t'+f'{formatted_key}: {value}\n')
                    f.write('\n')
            if 'coverage.Spatial' in astronomy_keys:
                cont+=1
                text = translate (astroProperties, astroPropertiesBack, 'datasetfieldtype.coverage.Spatial.title', False, False);
                f.write(str(cont)+'.  ' + text + ':\n')
                auxiliar=[]
                auxiliar.append(list_duplicates_of(astronomy_keys, 'coverage.Spatial'))
                for i in auxiliar[0]:
                    f.write('\t')
                    f.write(astronomy_values[i])
                    if i != auxiliar[0][-1]:
                        f.write('\n ')
                f.write('\n\n')
            if 'coverage.Depth' in astronomy_keys:
                cont+=1
                text = translate (astroProperties, astroPropertiesBack, 'datasetfieldtype.coverage.Depth.title', False, False);
                f.write(str(cont)+'.  ' + text + ':\n')
                f.write('\t'+astronomy_values[astronomy_keys.index('coverage.Depth')]+'\n\n')
            if 'coverage.ObjectDensity' in astronomy_keys:
                cont+=1
                text = translate (astroProperties, astroPropertiesBack, 'datasetfieldtype.coverage.ObjectDensity.title', False, False);
                f.write(str(cont)+'.  ' + text + ':\n')
                f.write('\t'+astronomy_values[astronomy_keys.index('coverage.ObjectDensity')]+'\n\n')
            if 'coverage.ObjectCount' in astronomy_keys:
                cont+=1
                text = translate (astroProperties, astroPropertiesBack, 'datasetfieldtype.coverage.ObjectCount.title', False, False);
                f.write(str(cont)+'.  ' + text + ':\n')
                f.write('\t'+astronomy_values[astronomy_keys.index('coverage.ObjectCount')]+'\n\n')
            if 'coverage.SkyFraction' in astronomy_keys:
                cont+=1
                text = translate (astroProperties, astroPropertiesBack, 'datasetfieldtype.coverage.SkyFraction.title', False, False);
                f.write(str(cont)+'.  ' + text + ':\n')
                f.write('\t'+astronomy_values[astronomy_keys.index('coverage.SkyFraction')]+'\n\n')
            if 'coverage.Polarization' in astronomy_keys:
                cont+=1
                text = translate (astroProperties, astroPropertiesBack, 'datasetfieldtype.coverage.Polarization.title', False, False);
                f.write(str(cont)+'.  ' + text + ':\n')
                f.write('\t'+astronomy_values[astronomy_keys.index('coverage.Polarization')]+'\n\n')
            if 'redshiftType' in astronomy_keys:
                cont+=1
                text = translate (astroProperties, astroPropertiesBack, 'datasetfieldtype.redshiftType.title', False, False);
                f.write(str(cont)+'.  ' + text + ':\n')
                f.write('\t'+astronomy_values[astronomy_keys.index('redshiftType')]+'\n\n')
            if 'resolution.Redshift' in astronomy_keys:
                cont+=1
                text = translate (astroProperties, astroPropertiesBack, 'datasetfieldtype.resolution.Redshift.title', False, False);
                f.write(str(cont)+'.  ' + text + ':\n')
                f.write('\t'+astronomy_values[astronomy_keys.index('resolution.Redshift')]+'\n\n')
            if  'coverage.Redshift.MinimumValue' in astronomy_keys or  'coverage.Redshift.MaximumValue' in astronomy_keys:
                cont+=1
                text = translate (astroProperties, astroPropertiesBack, 'datasetfieldtype.coverage.RedshiftValue.title', False, False);
                f.write(str(cont)+'.  ' + text + ':\n')
                index=[]
                keys=['coverage.Redshift.MinimumValue','coverage.Redshift.MaximumValue']
                specified_keys=[]
                specified_keys = [element for element in keys if element in astronomy_keys]
                extracted_values = find_keys(astronomy_keys, specified_keys, astronomy_values)
                for entry in extracted_values:
                    for key, value in entry.items():
                        formatted_key = translate (astroProperties, astroPropertiesBack, 'datasetfieldtype.' + key + '.title', False, False);
                        f.write('\t'+f'{formatted_key}: {value}\n')
                    f.write('\n')
        if len(biomedical_keys) != 0:
            text = translate (bioProperties, bioPropertiesBack, 'metadatablock.displayName', False, False);
            f.write(text + '\n--------------------------------------\n')
            cont=0
            if 'studyDesignType' in biomedical_keys:
                cont+=1
                text = translate (bioProperties, bioPropertiesBack, 'datasetfieldtype.studyDesignType.title', False, False);
                f.write(str(cont)+'.  ' + text + ':\n')
                auxiliar=[]
                auxiliar.append(list_duplicates_of(biomedical_keys, 'studyDesignType'))
                for i in auxiliar[0]:
                    f.write('\t')
                    f.write(biomedical_values[i])
                    if i != auxiliar[0][-1]:
                        f.write('\n ')
                f.write('\n\n')
            if 'studyFactorType' in biomedical_keys:
                cont+=1
                text = translate (bioProperties, bioPropertiesBack, 'datasetfieldtype.studyFactorType.title', False, False);
                f.write(str(cont)+'.  ' + text + ':\n')
                auxiliar=[]
                auxiliar.append(list_duplicates_of(biomedical_keys, 'studyFactorType'))
                for i in auxiliar[0]:
                    f.write('\t')
                    f.write(biomedical_values[i])
                    if i != auxiliar[0][-1]:
                        f.write('\n ')
                f.write('\n\n')
            if 'studyAssayOrganism' in biomedical_keys:
                cont+=1
                text = translate (bioProperties, bioPropertiesBack, 'datasetfieldtype.studyAssayOrganism.title', False, False);
                f.write(str(cont)+'.  ' + text + ':\n')
                auxiliar=[]
                auxiliar.append(list_duplicates_of(biomedical_keys, 'studyAssayOrganism'))
                for i in auxiliar[0]:
                    f.write('\t')
                    f.write(biomedical_values[i])
                    if i != auxiliar[0][-1]:
                        f.write('\n ')
                f.write('\n\n')
            if 'studyAssayOtherOrganism' in biomedical_keys:
                cont+=1
                text = translate (bioProperties, bioPropertiesBack, 'datasetfieldtype.studyAssayOtherOrganism.title', False, False);
                f.write(str(cont)+'.  ' + text + ':\n')
                auxiliar=[]
                auxiliar.append(list_duplicates_of(biomedical_keys, 'studyAssayOtherOrganism'))
                for i in auxiliar[0]:
                    f.write('\t')
                    f.write(biomedical_values[i])
                    if i != auxiliar[0][-1]:
                        f.write('\n ')
                f.write('\n\n')
            if 'studyAssayMeasurementType' in biomedical_keys:
                cont+=1
                text = translate (bioProperties, bioPropertiesBack, 'datasetfieldtype.studyAssayMeasurementType.title', False, False);
                f.write(str(cont)+'.  ' + text + ':\n')
                auxiliar=[]
                auxiliar.append(list_duplicates_of(biomedical_keys, 'studyAssayMeasurementType'))
                for i in auxiliar[0]:
                    f.write('\t')
                    f.write(biomedical_values[i])
                    if i != auxiliar[0][-1]:
                        f.write('\n ')
                f.write('\n\n')
            if 'studyAssayOtherMeasurmentType' in biomedical_keys:
                cont+=1
                text = translate (bioProperties, bioPropertiesBack, 'datasetfieldtype.studyAssayOtherMeasurmentType.title', False, False);
                f.write(str(cont)+'.  ' + text + ':\n')
                auxiliar=[]
                auxiliar.append(list_duplicates_of(biomedical_keys, 'studyAssayOtherMeasurmentType'))
                for i in auxiliar[0]:
                    f.write('\t')
                    f.write(biomedical_values[i])
                    if i != auxiliar[0][-1]:
                        f.write('\n ')
                f.write('\n\n')
            if 'studyAssayTechnologyType' in biomedical_keys:
                cont+=1
                text = translate (bioProperties, bioPropertiesBack, 'datasetfieldtype.studyAssayTechnologyType.title', False, False);
                f.write(str(cont)+'.  ' + text + ':\n')
                auxiliar=[]
                auxiliar.append(list_duplicates_of(biomedical_keys, 'studyAssayTechnologyType'))
                for i in auxiliar[0]:
                    f.write('\t')
                    f.write(biomedical_values[i])
                    if i != auxiliar[0][-1]:
                        f.write('\n ')
                f.write('\n\n')
            if 'studyAssayPlatform' in biomedical_keys:
                cont+=1
                text = translate (bioProperties, bioPropertiesBack, 'datasetfieldtype.studyAssayPlatform.title', False, False);
                f.write(str(cont)+'.  ' + text + ':\n')
                auxiliar=[]
                auxiliar.append(list_duplicates_of(biomedical_keys, 'studyAssayPlatform'))
                for i in auxiliar[0]:
                    f.write('\t')
                    f.write(biomedical_values[i])
                    if i != auxiliar[0][-1]:
                        f.write('\n ')
                f.write('\n\n')
            if 'studyAssayCellType' in biomedical_keys:
                cont+=1
                text = translate (bioProperties, bioPropertiesBack, 'datasetfieldtype.studyAssayCellType.title', False, False);
                f.write(str(cont)+'.  ' + text + ':\n')
                auxiliar=[]
                auxiliar.append(list_duplicates_of(biomedical_keys, 'studyAssayCellType'))
                for i in auxiliar[0]:
                    f.write('\t')
                    f.write(biomedical_values[i])
                    if i != auxiliar[0][-1]:
                        f.write('\n ')
                f.write('\n\n')
        if len(journal_keys) != 0:
            text = translate (journalProperties, journalPropertiesBack, 'metadatablock.displayName', False, False);
            f.write(text + '\n--------------------------------------\n')
            cont=0
            if 'journalVolume' in journal_keys or 'journalIssue' in journal_keys or 'journalPubDate' in journal_keys:
                cont+=1
                text = translate (journalProperties, journalPropertiesBack, 'datasetfieldtype.journalVolumeIssue.title', False, False);
                f.write(str(cont)+'.  ' + text + ':\n')
                index=[]
                keys=['journalVolume', 'journalIssue','journalPubDate']
                specified_keys=[]
                specified_keys = [element for element in keys if element in journal_keys]
                extracted_values = find_keys(journal_keys, specified_keys, journal_values)
                for entry in extracted_values:
                    for key, value in entry.items():
                        formatted_key = translate (journalProperties, journalPropertiesBack, 'datasetfieldtype.' + key + '.title', False, False);
                        f.write('\t'+f'{formatted_key}: {value}\n')
                    f.write('\n')
            if 'journalArticleType' in journal_keys:
                cont+=1
                text = translate (journalProperties, journalPropertiesBack, 'datasetfieldtype.journalArticleType.title', False, False);
                f.write(str(cont)+'.  ' + text + ':\n')
                f.write('\t'+journal_values[journal_keys.index('journalArticleType')]+'\n\n')

        if len(computationalworkflow_keys) != 0:
            text = translate (computationalProperties, computationalPropertiesBack, 'metadatablock.displayName', False, False);
            f.write(text + '\n--------------------------------------\n')
            cont=0
            if 'workflowType' in computationalworkflow_keys:
                cont+=1
                text = translate (computationalProperties, computationalPropertiesBack, 'datasetfieldtype.workflowType.title', False, False);
                f.write(str(cont)+'.  ' + text + ':\n')
                f.write('\t'+computationalworkflow_values[computationalworkflow_keys.index('workflowType')]+'\n\n')
            if 'workflowCodeRepository' in computationalworkflow_keys:
                cont+=1
                text = translate (computationalProperties, computationalPropertiesBack, 'datasetfieldtype.workflowCodeRepository.title', False, False);
                f.write(str(cont)+'.  ' + text + ':\n')
                auxiliar=[]
                auxiliar.append(list_duplicates_of(computationalworkflow_keys, 'workflowCodeRepository'))
                for i in auxiliar[0]:
                    f.write('\t')
                    f.write(computationalworkflow_values[i])
                    if i != auxiliar[0][-1]:
                        f.write('\n ')
                f.write('\n\n')
            if 'workflowCodeRepository' in computationalworkflow_keys:
                cont+=1
                text = translate (computationalProperties, computationalPropertiesBack, 'datasetfieldtype.workflowDocumentation.title', False, False);
                f.write(str(cont)+'.  ' + text + ':\n')
                auxiliar=[]
                auxiliar.append(list_duplicates_of(computationalworkflow_keys, 'workflowDocumentation'))
                for i in auxiliar[0]:
                    f.write('\t')
                    f.write(computationalworkflow_values[i])
                    if i != auxiliar[0][-1]:
                        f.write('\n ')
                f.write('\n\n')
        if len(LocalContextsCVoc_keys) != 0:
            text = translate (bundleProperties, bundlePropertiesBack, 'createReadme.localContextsMetadata', False, False);
            f.write(text + '\n--------------------------------------\n')
            cont=0
            if 'LCProjectUrl' in LocalContextsCVoc_keys:
                cont+=1
                text = translate (bundleProperties, bundlePropertiesBack, 'createReadme.localContextsURL', False, False);
                f.write(str(cont)+'.  ' + text + ':\n')
                f.write('\t'+LocalContextsCVoc_values[LocalContextsCVoc_keys.index('LCProjectUrl')]+'\n\n')

        text = translate (bundleProperties, bundlePropertiesBack, 'createReadme.fileOverview', False, False);
        f.write(text + '\n----------------------\n')
        for i in range(0,len(filemetadata_keys)):
            text = translate (bundleProperties, bundlePropertiesBack, 'file.fileName', False, False);
            f.write('\t' + text + ': '+filemetadata_values[i][filemetadata_keys[i].index('filename')]+'\n')
            if 'description' in filemetadata_keys[i]:
                text = translate (bundleProperties, bundlePropertiesBack, 'file.description.label', False, False);
                f.write('\t'+ text +': '+filemetadata_values[i][filemetadata_keys[i].index('description')]+'\n')
            # MADROÑO. Remove file format information (3 lines)
            f.write('\n')
            # text = translate (bundleProperties, bundlePropertiesBack, 'createReadme.fileFormat', False, False);
            # f.write('\t'+ text + ': '+filemetadata_values[i][filemetadata_keys[i].index('contentType')]+'\n\n')
        print('The Readme has been created in the directory ' + path +'.')

# Checking if both inputs are provided
if not doi or not token or not dest_dir or not base_url or not identifier or not properties_path or not language:
    print("Please enter DOI, Token, URL, destination, identifier, properties_path and language of the repository correctly.")
else:
    api = NativeApi(base_url, token)
    dataset = api.get_dataset(doi)

    #  Metadata lists:
    citation_keys, geo_keys, social_keys, astronomy_keys, biomedical_keys, journal_keys, computationalworkflow_keys, LocalContextsCVoc_keys, darwincore_keys  = [[] for _ in range(9)]
    citation_values, geo_values, social_values, astronomy_values, biomedical_values, journal_values, computationalworkflow_values, LocalContextsCVoc_values, darwincore_values = [[] for _ in range(9)]
    filemetadata_keys=[]
    filemetadata_values=[]

    # Exporting metadata and creating readme
    exportmetadata(base_url, token, doi, citation_keys, citation_values, geo_keys, geo_values, social_keys,
                   social_values, astronomy_keys, astronomy_values, biomedical_keys, biomedical_values,
                   journal_keys, journal_values,computationalworkflow_keys, computationalworkflow_values,
                   LocalContextsCVoc_keys, LocalContextsCVoc_values, darwincore_keys, darwincore_values)
    filemetadata(base_url, token, doi, filemetadata_keys, filemetadata_values)
    createreadme(base_url, token, doi, citation_keys, citation_values, geo_keys, geo_values, social_keys,
                 social_values, astronomy_keys, astronomy_values, biomedical_keys, biomedical_values,
                 journal_keys, journal_values, computationalworkflow_keys, computationalworkflow_values,
                 LocalContextsCVoc_keys, LocalContextsCVoc_values, darwincore_keys, darwincore_values,
                 filemetadata_keys, filemetadata_values)
