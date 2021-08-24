package br.com.zup.edu.keymanager.chavepix

import br.com.zup.edu.keymanager.chavepix.pix.TipoChave
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

internal class TipoChaveTest{

    @Nested
    inner class ChaveAleatoriaTest {

        @Test
        fun `DEVE ser valido quando chave ALEATORIA for nula ou vazia`() {
            val tipoChave = TipoChave.ALEATORIA

            assertTrue(tipoChave.valida(null))
            assertTrue(tipoChave.valida(""))
        }


        @Test
        fun `NAO deve ser valido quando chave ALEATORIA possuir valor`() {
            val tipoChave = TipoChave.ALEATORIA

            assertFalse(tipoChave.valida("chave"))
        }
    }


    @Nested
    inner class ChaveCpfTest{

        @Test
        fun `DEVE ser valido quando chave CPF for nula ou vazia`() {
            val tipoChave = TipoChave.CPF

            assertTrue(tipoChave.valida(null))
            assertTrue(tipoChave.valida(""))
        }


        @Test
        fun `NAO deve ser valido quando chave CPF possuir valor`() {
            val tipoChave = TipoChave.CPF

            assertFalse(tipoChave.valida("chave"))
        }

    }


    @Nested
    inner class ChaveCelularTest{

        @Test
        fun `DEVE ser valido quando tiver preenchimento E sem erros`(){
            val tipoChave = TipoChave.CELULAR

            assertTrue(tipoChave.valida("+5549968094131"))
            assertTrue(tipoChave.valida("+5549968094131"))
        }

    }


    @Nested
    inner class ChaveEmailTest{

        @Test
        fun `DEVE ser valido quando tiver preenchimento E sem erros`(){
            val tipoChave = TipoChave.EMAIL

            assertTrue(tipoChave.valida("teste@zup"))
            assertTrue(tipoChave.valida("Teste@Zup.com"))
        }


        @Test
        fun `NAO deve ser valido quando nao for um email invalido ou incompleto ou vazio`(){
            val tipoChave = TipoChave.EMAIL

            assertFalse(tipoChave.valida("teste"))
            assertFalse(tipoChave.valida(""))
            assertFalse(tipoChave.valida(null))
            assertFalse(tipoChave.valida("testezup.com"))
            assertFalse(tipoChave.valida("@zup.com"))
        }
    }
}