package com.mootann.arxivdaily.xml;

import jakarta.xml.bind.annotation.*;
import lombok.Data;

import java.util.List;

/**
 * arXiv Entry XML映射
 */
@Data
@XmlAccessorType(XmlAccessType.FIELD)
public class ArxivEntry {
    
    @XmlElement(name = "id", namespace = "http://www.w3.org/2005/Atom")
    private String id;
    
    @XmlElement(name = "title", namespace = "http://www.w3.org/2005/Atom")
    private String title;
    
    @XmlElement(name = "summary", namespace = "http://www.w3.org/2005/Atom")
    private String summary;
    
    @XmlElement(name = "published", namespace = "http://www.w3.org/2005/Atom")
    private String published;
    
    @XmlElement(name = "updated", namespace = "http://www.w3.org/2005/Atom")
    private String updated;
    
    @XmlElement(name = "author", namespace = "http://www.w3.org/2005/Atom")
    private List<ArxivAuthor> authors;
    
    @XmlElement(name = "primary_category", namespace = "http://arxiv.org/schemas/atom")
    private ArxivCategory primaryCategory;
    
    @XmlElement(name = "category", namespace = "http://www.w3.org/2005/Atom")
    private List<ArxivCategory> categories;
    
    @XmlElement(name = "doi", namespace = "http://arxiv.org/schemas/atom")
    private String doi;
}
