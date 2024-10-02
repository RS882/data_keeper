package compress.data_keeper.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "folder")
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Folder extends EntityInfo {

    @OneToMany(mappedBy = "folder")
    private Set<FileInfo> fileInfoSet = new HashSet<>();

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id")
    private User owner;
}
