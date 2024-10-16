package edu.harvard.iq.dataverse.harvest.server.xoai;

import com.lyncode.xml.exceptions.XmlWriteException;
import com.lyncode.xoai.dataprovider.exceptions.BadArgumentException;
import com.lyncode.xoai.dataprovider.exceptions.CannotDisseminateFormatException;
import com.lyncode.xoai.dataprovider.exceptions.CannotDisseminateRecordException;
import com.lyncode.xoai.dataprovider.exceptions.DoesNotSupportSetsException;
import com.lyncode.xoai.dataprovider.exceptions.HandlerException;
import com.lyncode.xoai.dataprovider.exceptions.NoMatchesException;
import com.lyncode.xoai.dataprovider.exceptions.NoMetadataFormatsException;
import com.lyncode.xoai.dataprovider.exceptions.OAIException;
import com.lyncode.xoai.dataprovider.handlers.VerbHandler;
import com.lyncode.xoai.dataprovider.handlers.results.ListItemsResults;
import com.lyncode.xoai.dataprovider.handlers.helpers.ItemHelper;
import com.lyncode.xoai.dataprovider.handlers.helpers.ItemRepositoryHelper;
import com.lyncode.xoai.dataprovider.handlers.helpers.SetRepositoryHelper;
import com.lyncode.xoai.dataprovider.model.Context;
import com.lyncode.xoai.dataprovider.model.Item;
import com.lyncode.xoai.dataprovider.model.MetadataFormat;
import com.lyncode.xoai.dataprovider.model.Set;
import com.lyncode.xoai.dataprovider.parameters.OAICompiledRequest;
import com.lyncode.xoai.dataprovider.repository.Repository;
import com.lyncode.xoai.model.oaipmh.Header;
import com.lyncode.xoai.model.oaipmh.ListRecords;
import com.lyncode.xoai.model.oaipmh.Metadata;
import com.lyncode.xoai.model.oaipmh.Record;
import com.lyncode.xoai.model.oaipmh.ResumptionToken;
import com.lyncode.xoai.xml.XSLPipeline;
import com.lyncode.xoai.xml.XmlWriter;
import edu.harvard.iq.dataverse.Dataset;

import javax.xml.stream.XMLStreamException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;

/**
 *
 * @author Leonid Andreev
 * 
 * This is Dataverse's own implementation of ListRecords Verb Handler
 * (used instead of the ListRecordsHandler provided by XOAI). 
 * It is customized to support the optimizations that allows 
 * Dataverse to directly output pre-exported metadata records to the output 
 * stream, bypassing expensive XML parsing and writing. 
 */
public class XlistRecordsHandler extends VerbHandler<ListRecords> {
    private static java.util.logging.Logger logger = java.util.logging.Logger.getLogger("XlistRecordsHandler");
    private final ItemRepositoryHelper itemRepositoryHelper;
    private final SetRepositoryHelper setRepositoryHelper;

    public XlistRecordsHandler(Context context, Repository repository) {
        super(context, repository);
        this.itemRepositoryHelper = new ItemRepositoryHelper(getRepository().getItemRepository());
        this.setRepositoryHelper = new SetRepositoryHelper(getRepository().getSetRepository());
    }

    @Override
    public ListRecords handle(OAICompiledRequest parameters) throws OAIException, HandlerException {
        XlistRecords res = new XlistRecords();
        int length = getRepository().getConfiguration().getMaxListRecords();

        if (parameters.hasSet() && !getRepository().getSetRepository().supportSets())
            throw new DoesNotSupportSetsException();

        int offset = getOffset(parameters);
        ListItemsResults result;
        if (!parameters.hasSet()) {
            if (parameters.hasFrom() && !parameters.hasUntil())
                result = itemRepositoryHelper.getItems(getContext(), offset,
                        length, parameters.getMetadataPrefix(),
                        parameters.getFrom());
            else if (!parameters.hasFrom() && parameters.hasUntil())
                result = itemRepositoryHelper.getItemsUntil(getContext(), offset,
                        length, parameters.getMetadataPrefix(),
                        parameters.getUntil());
            else if (parameters.hasFrom() && parameters.hasUntil())
                result = itemRepositoryHelper.getItems(getContext(), offset,
                        length, parameters.getMetadataPrefix(),
                        parameters.getFrom(), parameters.getUntil());
            else
                result = itemRepositoryHelper.getItems(getContext(), offset,
                        length, parameters.getMetadataPrefix());
        } else {
            if (!setRepositoryHelper.exists(getContext(), parameters.getSet())) {
            //    throw new NoMatchesException();
            }
            if (parameters.hasFrom() && !parameters.hasUntil())
                result = itemRepositoryHelper.getItems(getContext(), offset,
                        length, parameters.getMetadataPrefix(),
                        parameters.getSet(), parameters.getFrom());
            else if (!parameters.hasFrom() && parameters.hasUntil())
                result = itemRepositoryHelper.getItemsUntil(getContext(), offset,
                        length, parameters.getMetadataPrefix(),
                        parameters.getSet(), parameters.getUntil());
            else if (parameters.hasFrom() && parameters.hasUntil())
                result = itemRepositoryHelper.getItems(getContext(), offset,
                        length, parameters.getMetadataPrefix(),
                        parameters.getSet(), parameters.getFrom(),
                        parameters.getUntil());
            else {
                System.err.println("**************************** JUAN: length: " + length + ". offset: " + offset + "  ****************");
                if (itemRepositoryHelper == null)
                    System.err.println("**************************** JUAN: itemRepositoryHelper ES NULL ****************");
                if (getContext() == null)
                    System.err.println("**************************** JUAN: getContext() ES NULL ****************");
                if (parameters == null)
                    System.err.println("**************************** JUAN: parameters ES NULL ****************");
                if (parameters.getMetadataPrefix() == null)
                    System.err.println("**************************** JUAN: parameters.getMetadataPrefix() ES NULL ****************");
                if (parameters.getSet() == null)
                    System.err.println("**************************** JUAN: parameters.getSet() ES NULL ****************");
                    
                result = itemRepositoryHelper.getItems(getContext(), offset,
                        length, parameters.getMetadataPrefix(),
                        parameters.getSet());
            }
        }

        List<Item> results = result.getResults();
        if (results.isEmpty()) throw new NoMatchesException();
        for (Item i : results)
            res.withRecord(this.createRecord(parameters, i));


        ResumptionToken.Value currentResumptionToken = new ResumptionToken.Value();
        if (parameters.hasResumptionToken()) {
            currentResumptionToken = parameters.getResumptionToken();
        } else if (result.hasMore()) {
            currentResumptionToken = parameters.extractResumptionToken();
        }

        XresumptionTokenHelper resumptionTokenHelper = new XresumptionTokenHelper(currentResumptionToken,
                getRepository().getConfiguration().getMaxListRecords());
        res.withResumptionToken(resumptionTokenHelper.resolve(result.hasMore()));

        return res;
    }


    private int getOffset(OAICompiledRequest parameters) {
        if (!parameters.hasResumptionToken())
            return 0;
        if (parameters.getResumptionToken().getOffset() == null)
            return 0;
        return parameters.getResumptionToken().getOffset().intValue();
    }

    private Record createRecord(OAICompiledRequest parameters, Item item)
            throws BadArgumentException, CannotDisseminateRecordException,
            OAIException, NoMetadataFormatsException, CannotDisseminateFormatException {
        MetadataFormat format = getContext().formatForPrefix(parameters.getMetadataPrefix());
        Header header = new Header();
        
        Dataset dataset = ((Xitem)item).getDataset();
        Xrecord xrecord = new Xrecord().withFormatName(parameters.getMetadataPrefix()).withDataset(dataset);
        header.withIdentifier(item.getIdentifier());

        ItemHelper itemHelperWrap = new ItemHelper(item);
        header.withDatestamp(item.getDatestamp());
        for (Set set : itemHelperWrap.getSets(getContext(), getRepository().getFilterResolver()))
            header.withSetSpec(set.getSpec());
        if (item.isDeleted())
            header.withStatus(Header.Status.DELETED);

        xrecord.withHeader(header);
        xrecord.withMetadata(item.getMetadata());
        
        return xrecord;
    }


    private XSLPipeline toPipeline(Item item) throws XmlWriteException, XMLStreamException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        XmlWriter writer = new XmlWriter(output);
        Metadata metadata = item.getMetadata();
        metadata.write(writer);
        writer.close();
        return new XSLPipeline(new ByteArrayInputStream(output.toByteArray()), true);
    }
}
