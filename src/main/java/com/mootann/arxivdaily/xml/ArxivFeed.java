package com.mootann.arxivdaily.xml;

import jakarta.xml.bind.annotation.*;
import lombok.Data;

import java.util.List;

/**
 * arXiv API Feed XML映射
 */
@Data
@XmlRootElement(name = "feed", namespace = "http://www.w3.org/2005/Atom")
@XmlAccessorType(XmlAccessType.FIELD)
public class ArxivFeed {
    
    @XmlElement(name = "totalResults", namespace = "http://a9.com/-/spec/opensearch/1.1/")
    private Integer totalResults;
    
    @XmlElement(name = "startIndex", namespace = "http://a9.com/-/spec/opensearch/1.1/")
    private Integer startIndex;
    
    @XmlElement(name = "itemsPerPage", namespace = "http://a9.com/-/spec/opensearch/1.1/")
    private Integer itemsPerPage;
    
    @XmlElement(name = "entry", namespace = "http://www.w3.org/2005/Atom")
    private List<ArxivEntry> entries;
}
