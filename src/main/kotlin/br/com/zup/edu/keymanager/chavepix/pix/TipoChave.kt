package br.com.zup.edu.keymanager.chavepix.pix

enum class TipoChave {

    UNKNOWN_CHAVE {
        override fun valida(chave: String?): Boolean {
            return false
        }
    },

    CPF {
        override fun valida(chave: String?) =
            chave.isNullOrBlank() //CPF não deve ser preenchido pois é o próprio CPF cadastrado do cliente
    },

    CELULAR {
        override fun valida(chave: String?): Boolean {
            if (chave.isNullOrBlank()) {
                return false
            }
            return chave.matches("^\\+[1-9][0-9]\\d{1,14}\$".toRegex())
        }
    },

    EMAIL {
        override fun valida(chave: String?): Boolean {
            if (!chave.isNullOrBlank()) {
                val regex =
                    Regex("^[a-zA-Z0-9.!#\$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?" +
                            "(?:\\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*\$")
                if (chave.matches(regex)) return true
            }
            return false
        }

    },

    ALEATORIA {
        override fun valida(chave: String?) =
            chave.isNullOrBlank() // chave aleatória não deve ser preenchida pois é criada automaticamente
    },
    CNPJ {
        override fun valida(chave: String?): Boolean {
            return false
        }
    };

    abstract fun valida(chave: String?): Boolean




}