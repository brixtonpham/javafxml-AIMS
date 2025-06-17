package Project_ITSS.UpdateProduct.Entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CD extends Product{
    private long CD_id;
    private String TrackList;
    private String genre;
    private String recordLabel;
    private String artists;
    private String releaseDate;
}
