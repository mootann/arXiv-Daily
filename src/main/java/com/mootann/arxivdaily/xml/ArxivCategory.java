package com.mootann.arxivdaily.xml;

import jakarta.xml.bind.annotation.*;
import lombok.Data;

/**
 * arXiv Category XML映射
 */
@Data
@XmlAccessorType(XmlAccessType.FIELD)
public class ArxivCategory {
    
    @XmlAttribute(name = "term")
    private String term;
    
    @XmlAttribute(name = "scheme")
    private String scheme;
}
