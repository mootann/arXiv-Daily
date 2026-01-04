package com.mootann.arxivdaily.xml;

import jakarta.xml.bind.annotation.*;
import lombok.Data;

/**
 * arXiv Author XML映射
 */
@Data
@XmlAccessorType(XmlAccessType.FIELD)
public class ArxivAuthor {
    
    @XmlElement(name = "name", namespace = "http://www.w3.org/2005/Atom")
    private String name;
}
