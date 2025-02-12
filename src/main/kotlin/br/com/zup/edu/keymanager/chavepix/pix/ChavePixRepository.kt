package br.com.zup.edu.keymanager.chavepix.pix

import io.micronaut.data.annotation.Repository
import io.micronaut.data.jpa.repository.JpaRepository
import java.util.*

@Repository
interface ChavePixRepository : JpaRepository<ChavePix, UUID> {

    fun existsByChave(chave: String?): Boolean

    fun findByContaAssociadaTitularCpf(cpf: String): Optional<ChavePix>

    fun findByChave(chave: String): Optional<ChavePix>

    fun findByContaAssociadaTitularTitularId(id: UUID): Optional<ChavePix>

    fun findByIdAndContaAssociadaTitularTitularId(pixId: UUID, titularId: UUID): Optional<ChavePix>

    fun existsByContaAssociadaTitularTitularId(titularId: UUID): Boolean

    fun findAllByContaAssociadaTitularTitularId(titularId: UUID): List<ChavePix>

}
